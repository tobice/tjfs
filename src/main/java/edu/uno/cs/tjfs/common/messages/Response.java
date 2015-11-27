package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.ErrorResponseArgs;
import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;

public class Response extends Message{
    public MCode code;

    public Response (MCode code, IMessageArgs args, byte[] data, int dataLength){
        this.code = code;
        this.args = args;
        this.data = data;
        this.dataLength = dataLength;
    }

    public static Response Success(){
        return new Response(MCode.SUCCESS, null, null, 0);
    }

    public static Response Error(String status){
        return new Response(MCode.ERROR, new ErrorResponseArgs(status), null, 0);
    }

    public static Response Success(byte[] data){
        return new Response(MCode.SUCCESS, null, data, data.length);
    }

    public static Response Success(IMessageArgs args){
        return new Response(MCode.SUCCESS, args, null, 0);
    }
}