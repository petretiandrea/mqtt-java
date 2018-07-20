package it.petretiandrea.common;

import java.io.IOException;

public interface Transport {

    void write(byte[] data) throws IOException;

    int read(byte[] buffer) throws IOException;
}
