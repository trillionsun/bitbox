package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandGroup {
    public static Map<String, JSONObject>  CommandMap= new HashMap<>();
    public static String[] CommandName ={"INVALID_PROTOCOL", "CONNECTION_REFUSED","HANDSHAKE_REQUEST","HANDSHAKE_RESPONSE","FILE_CREATE_REQUEST","FILE_CREATE_RESPONSE"
    , "FILE_BYTES_REQUEST", "FILE_BYTES_RESPONSE", "FILE_DELETE_REQUEST", "FILE_DELETE_RESPONSE","FILE_MODIFY_REQUEST"
    ,"FILE_MODIFY_RESPONSE","DIRECTORY_CREATE_REQUEST","DIRECTORY_CREATE_RESPONSE","DIRECTORY_DELETE_REQUEST","DIRECTORY_DELETE_RESPONSE"
    };


    public static String[] CommandString={
            "{\"command\":\"INVALID_PROTOCOL\",\"message\":\"message must contain a command field as string\"}",

            "{\"command\":\"CONNECTION_REFUSED\",\"message\":\"connection limit reached\",\"peers\":{\"host\":\"sunrise.cis.unimelb.edu.au\",\"port\":8111}}",

            "{\"command\":\"HANDSHAKE_REQUEST\"," +
                    "\"hostPort\":{" +
                    "\"host\":\"sunrise.cis.unimelb.edu.au\"," +
                    "\"port\":8111" +
                    "}}",

            "{\"command\":\"HANDSHAKE_RESPONSE\"," +
                    "\"hostPort\":{" +
                    "\"host\":\"bigdata.cis.unimelb.edu.au\"," +
                    "\"port\":8500" +
                    "}}",

            "{\"command\":\"FILE_CREATE_REQUEST\"," +
                    "\"fileDescriptor\":{" +
                    "\"md5\":\"074195d72c47315efae797b69393e5e5\"," +
                    "\"lastModified\":1553417607000," +
                    "}," +
                    "\"fileSize\":45787" +
                    "\"pathName\":\"test.jpg\"}",

            "{\"command\":\"FILE_CREATE_RESPONSE\"," +
                    "\"fileDescriptor\":{" +
                    "\"md5\":\"074195d72c47315efae797b69393e5e5\"," +
                    "\"lastModified\":1553417607000," +
                    "}," +
                    "\"fileSize\":45787" +
                    "\"pathName\":\"test.jpg\"," +
                    "\"message\":\"file loader ready\"," +
                    "\"status\":true}",

            "{\"command\":\"FILE_BYTES_REQUEST\"," +
                    "\"fileDescriptor\":{" +
                    "\"md5\":\"b1946ac92492d2347c6235b4d2611184\"," +
                    "\"lastModified\":1553417607000," +
                    "}," +
                    "\"fileSize\":6" +
                    "\"pathName\":\"hello.txt\"," +
                    "\"position\":0," +
                    "\"length\":6}",

            "{\"command\":\"FILE_BYTES_RESPONSE\"," +
                    "\"fileDescriptor\":{" +
                    "\"md5\":\"b1946ac92492d2347c6235b4d2611184\"," +
                    "\"lastModified\":1553417607000," +
                    "\"fileSize\":6" +
                    "}," +
                    "\"pathName\":\"hello.txt\"," +
                    "\"position\":0," +
                    "\"length\":6," +
                    "\"content\":\"aGVsbG8K\"" +
                    "\"message\":\"successful read\"," +
                    "\"status\":true}",

            "{\"command\":\"FILE_DELETE_REQUEST\"," +
                    "\"fileDescriptor\":{" +
                    "}," +
                    "\"md5\":\"074195d72c47315efae797b69393e5e5\"," +
                    "\"lastModified\":1553417607000," +
                    "\"fileSize\":45787" +
                    "\"pathName\":\"test.jpg\"}",

            "{\"command\":\"FILE_DELETE_RESPONSE\"," +
                    "\"fileDescriptor\":{" +
                    "\"md5\":\"074195d72c47315efae797b69393e5e5\"," +
                    "\"lastModified\":1553417607000," +
                    "}," +
                    "\"fileSize\":45787" +
                    "\"pathName\":\"test.jpg\"," +
                    "\"message\":\"pathname does not exist\"," +
                    "\"status\":false}",

            "{\"command\":\"FILE_MODIFY_REQUEST\"," +
                    "\"fileDescriptor\":{" +
                    "}," +
                    "\"md5\":\"d35eab5dd9cb8b0d467c7e742c9b8c4c\"," +
                    "\"lastModified\":1553417617000," +
                    "\"fileSize\":46787" +
                    "\"pathName\":\"test.jpg\"}",

            "{\"command\":\"FILE_MODIFY_RESPONSE\"," +
                    "\"fileDescriptor\":{" +
                    "\"md5\":\"074195d72c47315efae797b69393e5e5\"," +
                    "\"lastModified\":1553417607000," +
                    "}," +
                    "\"fileSize\":45787" +
                    "\"pathName\":\"test.jpg\"," +
                    "\"message\":\"file loader ready\"," +
                    "\"status\":true}",

            "{\"command\":\"DIRECTORY_CREATE_REQUEST\"," +
                    "\"pathName\":\"dir/subdir/etc\"}",

            "{\"command\":\"DIRECTORY_CREATE_RESPONSE\"," +
                    "\"pathName\":\"dir/subdir/etc\"," +
                    "\"message\":\"pathname already exists\"," +
                    "\"status\":false}",


            "{\"command\":\"DIRECTORY_DELETE_REQUEST\"," +
                    " \"pathName\":\"dir/subdir/etc\"}",


            "{\"command\":\"DIRECTORY_DELETE_RESPONSE\"," +
                    "\"pathName\":\"dir/subdir/etc\"," +
                    "\"message\":\"directory deleted\"," +
                    "\"status\":true}"
    };





    public CommandGroup()
    {

       for(int i=0; i<CommandString.length; i++) {
            try {
              JSONObject json;
               json = (JSONObject) new JSONParser().parse(CommandString[i]);
                String key = CommandName[i];
              this.CommandMap.put(key, json);
            }catch (ParseException e)
            {
                System.out.println("parse failed");
            }
        }
   }

}
