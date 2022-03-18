/*-
 * Copyright (c) 2012-2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.javadeptools.rpm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

/**
 * A class for reading RPM package as an archive.
 * 
 * @author Mikolaj Izdebski
 */
public class RpmArchiveInputStream extends ArchiveInputStream {
    private final CpioArchiveInputStream delegate;

    public RpmArchiveInputStream(Path path) throws IOException {
        this.delegate = wrapFile(path);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public CpioArchiveEntry getNextEntry() throws IOException {
        return delegate.getNextCPIOEntry();
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return delegate.read(buf);
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        return delegate.read(buf, off, len);
    }

    private static IOException error(Path path, String message) throws IOException {
        throw new IOException("Unable to open RPM file " + path + ": " + message);
    }

    private static boolean hasGzipMagic(InputStream fis) throws IOException {
        try {
            fis.mark(2);
            return fis.read() == 31 && fis.read() == 139;
        } finally {
            fis.reset();
        }
    }

    private static CpioArchiveInputStream wrapFile(Path path) throws IOException {
        RpmInfo info = new RpmInfo(path);
        InputStream fis = new BufferedInputStream(Files.newInputStream(path));
        if (fis.skip(info.getHeaderSize()) != info.getHeaderSize()) {
            throw new IOException("Incomplete skip on archive stream");
        }
        
        InputStream cis;
        switch (info.getCompressionMethod()) {
        case "gzip":
            if (hasGzipMagic(fis))
                cis = new GzipCompressorInputStream(fis, true);
            else
                cis = fis;
            break;
        case "bzip2":
            cis = new BZip2CompressorInputStream(fis);
            break;
        case "xz":
            cis = new XZCompressorInputStream(fis);
            break;
        case "lzma":
            cis = new LZMACompressorInputStream(fis);
            break;
        case "zstd":
            cis = new ZstdCompressorInputStream(fis);
            break;
        default:
            fis.close();
            throw error(path, "Unsupported compression method: " + info.getCompressionMethod());
        }

        if (!info.getArchiveFormat().equals("cpio")) {
            cis.close();
            throw error(path, "Unsupported archive format: " + info.getArchiveFormat());
        }

        return new CpioArchiveInputStream(cis);
    }

}
