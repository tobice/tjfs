package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.ErrorResponseArgs;
import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;

public class Response extends Message{
    public final MCode code;

    public Response(MCode code, IMessageArgs args, byte[] data){
        super(args, data);
        this.code = code;
    }

    public Response(MCode code, IMessageArgs args) {
        super(args);
        this.code = code;
    }

    public static Response Success() {
        return new Response(MCode.SUCCESS, null);
    }

    public static Response Error(String status){
        return new Response(MCode.ERROR, new ErrorResponseArgs(status));
    }

    public static Response Success(byte[] data){
        return new Response(MCode.SUCCESS, null, data);
    }

    public static Response Success(IMessageArgs args){
        return new Response(MCode.SUCCESS, args);
    }
}
