package com.yy.androidlib.util.http;

import android.util.Pair;
import com.yy.androidlib.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.zip.Deflater;

public class HttpMultipartEntity implements HttpEntity {
    private static final String CONTENT_DISPOSITION   = "Content-Disposition";
    private static final String FORM_DATA = "form-data";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String CR_LF = "\r\n";
    private static final String TWO_DASHES = "--";
    private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final int INIT_BUF_SIZE = 4096;
    private static final int READ_BUF_SIZE = 1024;

    private final String boundary;
    private final Charset charset;
    private final Header contentType;

    private long length;
    private volatile boolean dirty;
    private List<Pair<String, String>> mStrings = new ArrayList<Pair<String, String>>();
    private Map<String, String> mFiles = new HashMap<String, String>();
    private Map<String, String> mZipData = new HashMap<String, String>();
    private Map<String, String> mZipFiles = new HashMap<String, String>();
    private Map<String, String> mFileBlocks = new HashMap<String, String>();
    private Map<String, String> mFileData = new HashMap<String, String>();

    private int mStartPos = 0;
    private int mPartSize = 0;
    private int mBlockIndex = 0;

    public HttpMultipartEntity(Charset charset) {
        this.boundary = generateBoundary();
        this.charset = charset;
        this.contentType = new BasicHeader(HTTP.CONTENT_TYPE, getContentTypeString());
    }

    public Header getContentType() {
        return contentType;
    }

    public long getContentLength() {
        if (dirty) {
            try {
                LengthCounter lengthCounter = new LengthCounter();
                writeStringParts(lengthCounter);
                writeZipData(lengthCounter);
                writeZipFile(lengthCounter);
                writeFileParts(lengthCounter);
                writeFileData(lengthCounter);
                writeFileBlockParts(lengthCounter);
                writeFinishPart(lengthCounter);
                length = lengthCounter.getLength();
                dirty = false;
            } catch (IOException e) {
                Logger.error(this, e);
            }
        }
        return length;
    }

    public void writeTo(OutputStream out) throws IOException {
        DataWriter writer = new DataWriter(out);
        writeStringParts(writer);
        writeZipData(writer);
        writeZipFile(writer);
        writeFileParts(writer);
        writeFileData(writer);
        writeFileBlockParts(writer);
        writeFinishPart(writer);
    }

    public void addStringPart(String name, String value) {
        Pair<String, String> pair = Pair.create(name, value);
        mStrings.add(pair);
        dirty = true;
    }

    public void addFileParts(String name, String filepath) {
        mFiles.put(filepath, name);
        dirty = true;
    }

    public void addZipData(String name, String data) {
        mZipData.put(name, data);
        dirty = true;
    }

    public void addZipFile(String name, String filePath) {
        mZipFiles.put(name, filePath);
        dirty = true;
    }

    public void addFileData(String name, String fileData) {
        mFileData.put(name, fileData);
        dirty = true;
    }

    public void addFileBlock(String name, String filepath, int startPos, int size, int index) {
        this.mStartPos = startPos;
        this.mPartSize = size;
        this.mBlockIndex = index;
        mFileBlocks.put(filepath, name);
        dirty = true;
    }

    private String getContentTypeString() {
        StringBuilder sb = new StringBuilder();
        sb.append(MULTIPART_FORM_DATA);
        sb.append("; ");
        sb.append("boundary=");
        sb.append(this.boundary);
        return sb.toString();
    }

    private interface IDataPolicy {
        void write(byte[] buffer, int offset, int count) throws IOException;
    }

    private static class LengthCounter implements IDataPolicy {
        private int length = 0;

        public int getLength() {
            return this.length;
        }

        public void write(byte[] buffer, int offset, int count) {
            this.length += count;
        }
    }

    private static class DataWriter implements IDataPolicy {
        private OutputStream out;
        public DataWriter(OutputStream out) {
            this.out = out;
        }

        public void write(byte[] buffer, int offset, int count) throws IOException {
            out.write(buffer, offset, count);
        }
    }

    private void writeFileHeader(IDataPolicy dataPolicy, String mimeType,
                                 String name, String uploadFileName) throws IOException {
        writeBoundary(dataPolicy);
        StringBuilder sb = new StringBuilder();
        sb.append(CONTENT_DISPOSITION);
        sb.append(": ");
        sb.append(FORM_DATA);
        sb.append("; ");
        sb.append("name=\"");
        sb.append(name);
        sb.append("\"; ");
        sb.append("filename=\"");
        sb.append(uploadFileName);
        sb.append("\"");
        sb.append(CR_LF);
        sb.append(HTTP.CONTENT_TYPE + ": ");
        sb.append(mimeType);
        sb.append(CR_LF);
        sb.append(CR_LF);
        writeBytes(sb.toString(), dataPolicy);
    }

    private void writeZipFile(IDataPolicy dataPolicy) throws IOException {
        for (Entry<String, String> entry : mZipFiles.entrySet()) {
            String mimeType = BasicFileUtils.getFileMime(BasicFileUtils.ZIP_EXT);
            String name = entry.getKey();
            writeFileHeader(dataPolicy, mimeType, name, entry.getValue());
            addZlibDataFromFile(dataPolicy, entry.getValue());
        }
    }

    private void addZlibDataFromFile(IDataPolicy dataPolicy, String fileName) throws IOException {
        Deflater compressor = new Deflater();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            int bytes = 0;
            byte[] buffer = new byte[INIT_BUF_SIZE];
            while ((bytes = fis.read(buffer)) != -1) {
                compressor.setInput(buffer, 0, bytes);
            }
            compressor.finish();
            bytes = 0;
            while (!compressor.finished()) {
                bytes = compressor.deflate(buffer);
                dataPolicy.write(buffer, 0, bytes);
            }
            compressor.end();
        }
        catch (Exception e) {
            Logger.error(this, "addZlibDataFromFile, %s", e);
        }
        finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    private void writeZipData(IDataPolicy dataPolicy) throws IOException {
//        for (Entry<String, String> entry : mZipData.entrySet()) {
////            String mimeType = BasicFileUtils.getFileMime(BasicFileUtils.ZIP_EXT);
//            String name = entry.getKey();
//            writeFileHeader(dataPolicy, mimeType, name, name);
//            addZlibCompressData(dataPolicy, entry.getValue());
//            writeBytes(CR_LF, dataPolicy);
//        }
    }

    private void addZlibCompressData(IDataPolicy dataPolicy, String data) throws IOException {
        Deflater compressor = new Deflater();
        compressor.setInput(data.getBytes());
        compressor.finish();
        int bytes = 0;
        byte[] buffer = new byte[INIT_BUF_SIZE];
        while (!compressor.finished()) {
            bytes = compressor.deflate(buffer);
            dataPolicy.write(buffer, 0, bytes);
        }
        compressor.end();
    }

    private void writeFileParts(IDataPolicy dataPolicy) throws IOException {
        for (String filePath : mFiles.keySet()) {
            final String mimeType = BasicFileUtils.getFileMime(filePath);
            final String name = mFiles.get(filePath);
            String uploadFileName = BasicFileUtils.getFileName(filePath);
            writeFileHeader(dataPolicy, mimeType, name, uploadFileName);
            File f = new File(filePath);
            FileInputStream in = null;
            try {
                in = new FileInputStream(f);
                byte[] buffer = new byte[INIT_BUF_SIZE];
                int readCount = 0;
                while ((readCount = in.read(buffer)) != -1) {
                    dataPolicy.write(buffer, 0, readCount);
                }
                writeBytes(CR_LF, dataPolicy);
            }
            finally {   
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    private void writeFileData(IDataPolicy dataPolicy) throws IOException {
        for (Entry<String, String> entry : mFileData.entrySet()) {
            String mimeType = BasicFileUtils.getFileMime(BasicFileUtils.ZIP_EXT);
            String name = entry.getKey();
            writeFileHeader(dataPolicy, mimeType, "files", name);
            dataPolicy.write(entry.getValue().getBytes(), 0, entry.getValue().length());
            writeBytes(CR_LF, dataPolicy);
        }
    }

    private void writeFileBlockParts(IDataPolicy dataPolicy) throws IOException {
        if (mPartSize == 0) {
            return ;
        }
        for (String filePath : mFileBlocks.keySet()) {
            final String mimeType = BasicFileUtils.getFileMime(filePath);
            String uploadFilename = BasicFileUtils.getFileName(filePath);
            writeBoundary(dataPolicy);
            StringBuilder sb0 = new StringBuilder();
            sb0.append(CONTENT_DISPOSITION);
            sb0.append(": ");
            sb0.append(FORM_DATA);
            sb0.append("; ");
            sb0.append("name=\"");
            sb0.append("block");
            sb0.append("\"; ");
            sb0.append(CR_LF);
            sb0.append(CR_LF);
            sb0.append(mBlockIndex);
            writeBytes(sb0.toString(), dataPolicy);
            writeBytes(CR_LF, dataPolicy);

            writeFileHeader(dataPolicy, mimeType, mFileBlocks.get(filePath), uploadFilename);

            File f = new File(filePath);
            FileInputStream in = null;
            try {
                in = new FileInputStream(f);
                byte[] buffer = new byte[READ_BUF_SIZE];

                in.skip(mStartPos);

                int readCount = 0, tmp = 0;
                while (tmp < mPartSize && ((readCount = in.read(buffer)) != -1)) {
                    if (readCount > mPartSize) {
                        dataPolicy.write(buffer, 0, mPartSize);
                    }
                    else {
                        dataPolicy.write(buffer, 0, readCount);
                    }
                    tmp += readCount;
                }

                writeBytes(CR_LF, dataPolicy);
            }
            finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    private void writeStringParts(IDataPolicy dataPolicy) throws IOException {
        for (Pair<String, String> pair : mStrings) {
            writeBoundary(dataPolicy);
            StringBuilder sb = new StringBuilder();
            sb.append(CONTENT_DISPOSITION);
            sb.append(": ");
            sb.append(FORM_DATA);
            sb.append("; ");
            sb.append("name=\"");
            sb.append(pair.first);
            sb.append("\"");
            sb.append(CR_LF);
            sb.append(CR_LF);
            sb.append(pair.second);
            sb.append(CR_LF);
            writeBytes(sb.toString(), dataPolicy);
        }
    }

    private void writeFinishPart(IDataPolicy dataPolicy) throws IOException {
        writeBytes(TWO_DASHES, dataPolicy);
        writeBytes(boundary, dataPolicy);
        writeBytes(TWO_DASHES, dataPolicy);
        writeBytes(CR_LF, dataPolicy);
    }

    private void writeBoundary(IDataPolicy dataPolicy) throws IOException {
        writeBytes(encode(TWO_DASHES), dataPolicy);
        writeBytes(encode(boundary), dataPolicy);
        writeBytes(encode(CR_LF), dataPolicy);
    }

    private void writeBytes(final String str, final IDataPolicy dataPolicy) throws IOException {
        writeBytes(encode(str), dataPolicy);
    }

    private void writeBytes(final ByteArrayBuffer b, IDataPolicy dataPolicy) throws IOException {
        dataPolicy.write(b.buffer(), 0, b.length());
    }

    private ByteArrayBuffer encode(final String string) {
        ByteBuffer encoded = charset.encode(CharBuffer.wrap(string));
        ByteArrayBuffer bab = new ByteArrayBuffer(encoded.remaining());
        bab.append(encoded.array(), encoded.position(), encoded.remaining());
        return bab;
    }

    private String generateBoundary() {
        StringBuilder buffer = new StringBuilder();
        Random rand = new Random();
        int count = rand.nextInt(11) + 30; // a random size from 30 to 40
        for (int i = 0; i < count; i++) {
            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        return buffer.toString();
    }

    //below functions are not used for multipart data

    public void consumeContent() throws IOException {
        throw new IOException("consumeContent is not supported!");
    }

    public InputStream getContent() throws IOException, IllegalStateException {
        throw new IOException("getContent is not supported!");
    }

    public Header getContentEncoding() {
        return null;
    }

    public boolean isChunked() {
        return !isRepeatable();
    }

    public boolean isRepeatable() {
        return true;
    }

    public boolean isStreaming() {
        return !isRepeatable();
    }
}
