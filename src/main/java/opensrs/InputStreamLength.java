/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensrs;

import java.io.InputStream;

/**
 * A simple wrapper around InputStream which also keeps track of the original length
 * of the data which should be readable from the stream.
 * @author piotr
 */
public class InputStreamLength {
    private InputStream inputStream;
    private long length;

    public InputStreamLength(InputStream inputStream, long length) {
        this.inputStream = inputStream;
        this.length = length;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
    
}
