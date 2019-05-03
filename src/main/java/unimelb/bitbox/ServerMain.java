package unimelb.bitbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.EVENT;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
	}

	// synchronazation after handshake with connected peers
	public void getSync() {
		Integer interval = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
		while (!ConnectionHost.getConnectedPeers().isEmpty()) {
			try {
				Thread.sleep(interval * 1000);
				ArrayList<FileSystemEvent> pathevents = new ArrayList<FileSystemEvent>();
				pathevents = fileSystemManager.generateSyncEvents();
				for (FileSystemEvent e : pathevents) {
					processFileSystemEvent(e);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		EVENT event = fileSystemEvent.event;
		JSONObject json = new JSONObject();

		switch (event) {
			case FILE_CREATE:
				json = getFileRequestFormat(fileSystemEvent);
				sendToAllPeers(json);
				break;

			case FILE_MODIFY:
				json = getFileRequestFormat(fileSystemEvent);
				sendToAllPeers(json);
				break;

			case FILE_DELETE:
				json = getFileRequestFormat(fileSystemEvent);
				sendToAllPeers(json);
				break;

			case DIRECTORY_CREATE:
				json = getDirRequestFormat(fileSystemEvent);
				sendToAllPeers(json);
				break;

			case DIRECTORY_DELETE:
				json = getDirRequestFormat(fileSystemEvent);
				sendToAllPeers(json);
				break;

			default:
				break;
		}
	}

	public void sendToAllPeers(JSONObject json) {
		try {
			ConnectionHost.sendAll(json);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public JSONObject getDirRequestFormat(FileSystemEvent fileSystemEvent) {
		/**
		 * DIRECTORY_CREATE_REQUEST, DIRECTORY_DELETE_REQUEST
		 */
		Document doc = new Document();
		doc.append("command", fileSystemEvent.event.toString() + "_REQUEST");
		doc.append("pathName", fileSystemEvent.pathName);
		JSONObject json = new JSONObject();
		try {
			json = (JSONObject) new JSONParser().parse(doc.toJson());
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return json;
	}

	public JSONObject getFileRequestFormat(FileSystemEvent fileSystemEvent) {
		/**
		 * FILE_CREATE_REQUEST, FILE_DELETE_REQUEST, FILE_MODIFY_REQUEST
		 */
		JSONObject json = new JSONObject();
		JSONObject des = new JSONObject();
		des.put("md5", fileSystemEvent.fileDescriptor.md5);
		des.put("lastModified", fileSystemEvent.fileDescriptor.lastModified);
		des.put("fileSize", fileSystemEvent.fileDescriptor.fileSize);

		json.put("fileDescriptor", des);
		json.put("command", fileSystemEvent.event.toString() + "_REQUEST");
		json.put("pathName", fileSystemEvent.pathName);

		return json;
	}

	// response for file creating
	public JSONObject fileCreateResponse(JSONObject request) throws NoSuchAlgorithmException, IOException {
		JSONObject json = new JSONObject();
		JSONObject description = new JSONObject();

		// resolve request
		JSONObject des = (JSONObject) request.get("fileDescriptor");
		String name = (String) request.get("pathName");
		String md5 = (String) des.get("md5");
		long lm = (long) des.get("lastModified");
		long size = (long) des.get("fileSize");

		description.put("md5", md5);
		description.put("lastModified", lm);
		description.put("fileSize", size);
		json.put("command", "FILE_CREATE_RESPONSE");
		json.put("pathName", name);
		json.put("fileDescriptor", description);

		// check for ready
		json.put("status", "false");
		if (fileSystemManager.isSafePathName(name)) {
			if (!fileSystemManager.fileNameExists(name)) {
				if (fileSystemManager.createFileLoader(name, md5, size, lm)) {
					json.put("message", "file loader ready");
					json.replace("status", "false", "true");
				} else {
					json.put("message", "there was a problem creating the file");
				}
			} else {
				json.put("message", "pathname already exists");
			}

		} else {
			json.put("message", "unsafe pathname given");
		}

		return json;
	}

	// response for file modifying
	public JSONObject fileModifyResponse(JSONObject request) throws NoSuchAlgorithmException, IOException {
		JSONObject json = new JSONObject();
		JSONObject description = new JSONObject();

		// resolve request
		JSONObject des = (JSONObject) request.get("fileDescriptor");
		String name = (String) request.get("pathName");
		String md5 = (String) des.get("md5");
		long lm = (long) des.get("lastModified");
		long size = (long) des.get("fileSize");

		description.put("md5", md5);
		description.put("lastModified", lm);
		description.put("fileSize", size);
		json.put("command", "FILE_MODIFY_RESPONSE");
		json.put("pathName", name);
		json.put("fileDescriptor", description);

		// check for ready
		boolean ready = false;
		if (fileSystemManager.isSafePathName(name)) {
			if (fileSystemManager.fileNameExists(name)) {
				if (fileSystemManager.modifyFileLoader(name, md5, lm)) {
					json.put("message", "file loader ready");
					ready = true;
				} else {
					json.put("message", "there was a problem modifying the file");
				}
			} else {
				json.put("message", "pathname does not exist");
			}

		} else {
			json.put("message", "unsafe pathname given");
		}
		json.put("status", ready);

		return json;
	}

	// response for file deletion
	public JSONObject fileDeleteResponse(JSONObject request) throws NoSuchAlgorithmException, IOException {
		JSONObject json = new JSONObject();
		JSONObject description = new JSONObject();

		// resolve request
		JSONObject des = (JSONObject) request.get("fileDescriptor");
		String name = (String) request.get("pathName");
		String md5 = (String) des.get("md5");
		long lm = (long) des.get("lastModified");
		long size = (long) des.get("fileSize");

		description.put("md5", md5);
		description.put("lastModified", lm);
		description.put("fileSize", size);
		json.put("command", "FILE_DELETE_RESPONSE");
		json.put("pathName", name);
		json.put("fileDescriptor", description);

		// check for ready
		boolean ready = false;
		if (fileSystemManager.isSafePathName(name)) {
			if (fileSystemManager.fileNameExists(name)) {
				if (fileSystemManager.deleteFile(name, lm, md5)) {
					json.put("message", "file deleted");
					ready = true;
				} else {
					json.put("message", "there was a problem deleting the file");
				}
			} else {
				json.put("message", "pathname does not exist");
			}

		} else {
			json.put("message", "unsafe pathname given");
		}
		json.put("status", ready);

		return json;
	}

	// prepare the file_bytes_request
	public JSONObject fileBytesRequest(JSONObject response) {
		JSONObject json = new JSONObject();
		String pathName = response.get("pathName").toString();

		// check shortcut before sending request
		Boolean shortcut = false;
		try {
			shortcut = fileSystemManager.checkShortcut(pathName);
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (!shortcut) {
			json.put("command", "FILE_BYTES_REQUEST");
			JSONObject initialDes = (JSONObject) response.get("fileDescriptor");
			long length = 0;

			// response -> FILE_CREATE_RESPONSE & FILE_MODIFY_RESPONSE
			if (response.get("command") == "FILE_CREATE_RESPONSE"
					|| response.get("command") == "FILE_MODIFY_RESPONSE") {
				json.put("position", 0);

				length = Integer.parseInt(Configuration.getConfigurationValue("blockSize"));
				String fs = initialDes.get("fileSize").toString();
				Integer fs_ = Integer.parseInt(fs);

				// if real fileSize < 10, change length to the fileSize
				if (fs_ < length) {
					length = fs_;
				}

				// response -> FILE_BYTES_RESPONSE
			} else {
				// base64 decode content
				String content = response.get("content").toString();
				byte[] decodedBytes = Base64.getDecoder().decode(content);
				ByteBuffer buf = ByteBuffer.wrap(decodedBytes);

				// format data
				String pos_ = response.get("position").toString();
				long position = (long) Integer.parseInt(pos_);
				String len_ = response.get("length").toString();
				length = Integer.parseInt(len_);

				try {
					fileSystemManager.writeFile(pathName, buf, position);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				json.put("position", position + length);
			}

			boolean finish = false;
			try {
				finish = fileSystemManager.checkWriteComplete(pathName);
				if (finish) {
					return new JSONObject();
				}
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			JSONObject des = new JSONObject();
			des.put("md5", initialDes.get("md5"));
			des.put("lastModified", initialDes.get("lastModified"));
			json.put("length", length);
			json.put("pathName", response.get("pathName"));
			json.put("fileSize", initialDes.get("fileSize"));
			json.put("fileDescriptor", des);

		}

		return json;
	}

	// prepare the file_bytes_response
	public JSONObject fileBytesResponse(JSONObject request) {
		JSONObject json = new JSONObject();
		JSONObject des = new JSONObject();
		JSONObject initialDes = (JSONObject) request.get("fileDescriptor");

		des.put("md5", initialDes.get("md5"));
		des.put("lastModified", initialDes.get("lastModified"));
		des.put("fileSize", request.get("fileSize"));

		json.put("command", "FILE_BYTES_RESPONSE");
		json.put("fileDescriptor", des);
		json.put("pathName", request.get("pathName"));
		json.put("position", request.get("position"));
		json.put("length", request.get("length"));

		ByteBuffer bb = null;
		String md5 = (String) initialDes.get("md5");
		String position_ = request.get("position").toString();
		long position = (long) Integer.parseInt(position_);
		String length_ = request.get("length").toString();
		long length = (long) Integer.parseInt(length_);
		long fileSize = Integer.parseInt(request.get("fileSize").toString());

		if (fileSize - position < length) {
			length = fileSize - position;
		}

		try {
			bb = fileSystemManager.readFile(md5, position, length);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (bb == null) {
			json.put("message", "unsuccessful read");
			json.put("status", false);
		} else {
			// base64 encode
			byte[] bt = bb.array();
			String content = Base64.getEncoder().encodeToString(bt);
			json.put("content", content);
			json.put("message", "successful read");
			json.put("status", true);
		}

		return json;
	}

	public JSONObject dirCreateResponse(JSONObject request) throws NoSuchAlgorithmException, IOException {
		JSONObject json = new JSONObject();

		// resolve request
		String name = (String) request.get("pathName");
		json.put("command", "DIRECTORY_CREATE_RESPONSE");
		json.put("pathName", name);

		boolean ready = false;
		if (fileSystemManager.isSafePathName(name)) {
			if (!fileSystemManager.dirNameExists(name)) {
				if (fileSystemManager.makeDirectory(name)) {
					json.put("message", "directory created");
					ready = true;
				} else {
					json.put("message", "there was a problem creating the directory");
				}
			} else {
				json.put("message", "pathname already exists");
			}

		} else {
			json.put("message", "unsafe pathname given");
		}
		json.put("status", ready);
		return json;
	}

	// response for directory deleting
	public JSONObject dirDeleteResponse(JSONObject request) throws NoSuchAlgorithmException, IOException {
		JSONObject json = new JSONObject();

		// resolve request
		String name = (String) request.get("pathName");
		json.put("command", "DIRECTORY_DELETE_RESPONSE");
		json.put("pathName", name);

		boolean ready = false;
		if (fileSystemManager.isSafePathName(name)) {
			if (fileSystemManager.dirNameExists(name)) {
				if (fileSystemManager.deleteDirectory(name)) {
					json.put("message", "directory deleted");
					ready = true;
				} else {
					json.put("message", "there was a problem deleting the directory");
				}
			} else {
				json.put("message", "pathname does not exist");
			}

		} else {
			json.put("message", "unsafe pathname given");
		}
		json.put("status", ready);

		return json;
	}

}
