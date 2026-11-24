package com.acertainbookstore.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.interfaces.BookStoreSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * Serializes bookstore request and response objects using Kryo.
 */
public final class BookStoreKryoSerializer implements BookStoreSerializer {

    private final Kryo binaryStream;

    public BookStoreKryoSerializer() {
        binaryStream = new Kryo();
        binaryStream.setInstantiatorStrategy(
                new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        // Register all classes exchanged by the bookstore RPC interface.
        binaryStream.register(ImmutableStockBook.class);
        binaryStream.register(HashSet.class);
        binaryStream.register(BookStoreResponse.class);
        binaryStream.register(com.acertainbookstore.business.BookCopy.class);
        binaryStream.register(java.util.ArrayList.class);
        binaryStream.register(com.acertainbookstore.business.BookEditorPick.class);
        binaryStream.register(com.acertainbookstore.business.ImmutableBook.class);

        // Only the message is part of the RPC error response. Do not ask Kryo
        // to reflect into Throwable's JDK-internal cause/stack-trace fields.
        binaryStream.register(BookStoreException.class,
                new Serializer<BookStoreException>() {
                    @Override
                    public void write(Kryo kryo, Output output, BookStoreException exception) {
                        output.writeString(exception.getMessage());
                    }

                    @Override
                    public BookStoreException read(Kryo kryo, Input input,
                            Class<? extends BookStoreException> type) {
                        return new BookStoreException(input.readString());
                    }
                });

        binaryStream.register(BookRating.class);
    }

    @Override
    public byte[] serialize(Object object) throws IOException {
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                Output out = new Output(outStream)) {
            binaryStream.writeClassAndObject(out, object);
            out.flush();
            return outStream.toByteArray();
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws IOException {
        try (InputStream inStream = new ByteArrayInputStream(bytes);
                Input in = new Input(inStream)) {
            return binaryStream.readClassAndObject(in);
        }
    }
}
