/*-
 * Copyright (c) 2012-2016 Red Hat, Inc.
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

import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.ValueLayout;

/**
 * @author Mikolaj Izdebski
 */
final class Rpm {

    static final int RPMRC_OK = 0;
    static final int RPMRC_NOTFOUND = 1;
    static final int RPMRC_FAIL = 2;
    static final int RPMRC_NOTTRUSTED = 3;
    static final int RPMRC_NOKEY = 4;

    static final int RPMVSF_NOHDRCHK = 1 << 0;
    static final int RPMVSF_NOSHA1HEADER = 1 << 8;
    static final int RPMVSF_NODSAHEADER = 1 << 10;
    static final int RPMVSF_NORSAHEADER = 1 << 11;
    static final int RPMVSF_NOMD5 = 1 << 17;
    static final int RPMVSF_NODSA = 1 << 18;
    static final int RPMVSF_NORSA = 1 << 19;

    static final int RPMTAG_NAME = 1000;
    static final int RPMTAG_VERSION = 1001;
    static final int RPMTAG_RELEASE = 1002;
    static final int RPMTAG_EPOCH = 1003;
    static final int RPMTAG_ARCH = 1022;
    static final int RPMTAG_SOURCERPM = 1044;
    static final int RPMTAG_PROVIDENAME = 1047;
    static final int RPMTAG_REQUIRENAME = 1049;
    static final int RPMTAG_CONFLICTNAME = 1054;
    static final int RPMTAG_OBSOLETENAME = 1090;
    static final int RPMTAG_SOURCEPACKAGE = 1106;
    static final int RPMTAG_PAYLOADCOMPRESSOR = 1125;
    static final int RPMTAG_PAYLOADFORMAT = 1124;
    static final int RPMTAG_ORDERNAME = 5035;
    static final int RPMTAG_RECOMMENDNAME = 5046;
    static final int RPMTAG_SUGGESTNAME = 5049;
    static final int RPMTAG_SUPPLEMENTNAME = 5052;
    static final int RPMTAG_ENHANCENAME = 5055;

    static final int HEADERGET_MINMEM = 1 << 0;

    static final int RPMDBI_INSTFILENAMES = 5040;

    private static final MemoryAddress toCStringAddress(String string, ResourceScope scope) {
        if (string == null) {
            return MemoryAddress.NULL;
        } else {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(string);
            var segment = MemorySegment.allocateNative(buffer.capacity() + 1, scope);
            segment.asByteBuffer().put(buffer);
            return segment.address();
        }
    }

    private static final String toJavaString(MemoryAddress address) {
        if (address.equals(MemoryAddress.NULL)) {
            return null;
        } else {
            return address.getUtf8String(0);
        }
    }

    private static class Library {
        private final SymbolLookup lookup;
        private final CLinker clinker;

        Library(String... libraries) {
            /*
             * Libraries and fields have to be initialized in this parent
             * constructor before inherited final method handles are initialized
             */
            lookup = SymbolLookup.loaderLookup();
            clinker = CLinker.systemCLinker();

            for (String library : libraries) {
                System.loadLibrary(library);
            }
        }

        final MethodHandle downcallHandle(String symbol, FunctionDescriptor descriptor) {
            return clinker.downcallHandle(lookup.lookup(symbol).get(), descriptor);
        }
    }

    private static class RpmLib extends Library {
        RpmLib() {
            super("rpm");
        }

        final MethodHandle rpmtsCreate = downcallHandle("rpmtsCreate",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
        final MethodHandle rpmtsFree = downcallHandle("rpmtsFree",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        final MethodHandle rpmtsSetVSFlags = downcallHandle("rpmtsSetVSFlags",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        final MethodHandle rpmtsSetRootDir = downcallHandle("rpmtsSetRootDir",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        final MethodHandle rpmtsInitIterator = downcallHandle("rpmtsInitIterator",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        final MethodHandle rpmdbFreeIterator = downcallHandle("rpmdbFreeIterator",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        final MethodHandle rpmdbNextIterator = downcallHandle("rpmdbNextIterator",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        final MethodHandle rpmReadPackageFile = downcallHandle("rpmReadPackageFile",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        final MethodHandle rpmReadConfigFiles = downcallHandle("rpmReadConfigFiles",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        final MethodHandle headerFree = downcallHandle("headerFree",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        final MethodHandle headerGet = downcallHandle("headerGet",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        final MethodHandle headerGetString = downcallHandle("headerGetString",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        final MethodHandle headerGetNumber = downcallHandle("headerGetNumber",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        final MethodHandle rpmtdNew = downcallHandle("rpmtdNew",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
        final MethodHandle rpmtdFree = downcallHandle("rpmtdFree",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        final MethodHandle rpmtdFreeData = downcallHandle("rpmtdFreeData",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        final MethodHandle rpmtdCount = downcallHandle("rpmtdCount",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        final MethodHandle rpmtdNext = downcallHandle("rpmtdNext",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        final MethodHandle rpmtdGetString = downcallHandle("rpmtdGetString",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    }

    private static class RpmIO extends Library {
        RpmIO() {
            super("rpmio");
        }

        final MethodHandle Fopen = downcallHandle("Fopen",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        final MethodHandle Fclose = downcallHandle("Fclose",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        final MethodHandle Ftell = downcallHandle("Ftell",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        final MethodHandle Ferror = downcallHandle("Ferror",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
        final MethodHandle Fstrerror = downcallHandle("Fstrerror",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    }

    private static class Lazy {
        static final RpmLib RPM = new RpmLib();
    }

    private static class LazyIO {
        static final RpmIO RPMIO = new RpmIO();
    }

    static final MemoryAddress rpmtsCreate() {
        try {
            return (MemoryAddress) Lazy.RPM.rpmtsCreate.invoke();
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final void rpmtsFree(MemoryAddress ts) {
        try {
            Lazy.RPM.rpmtsFree.invoke(ts);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final int rpmtsSetVSFlags(MemoryAddress ts, int flags) {
        try {
            return (int) Lazy.RPM.rpmtsSetVSFlags.invoke(ts, flags);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final int rpmtsSetRootDir(MemoryAddress ts, String rootDir) {
        try (var rootDirScope = ResourceScope.newConfinedScope()) {
            return (int) Lazy.RPM.rpmtsSetRootDir.invoke(
                    ts,
                    toCStringAddress(rootDir, rootDirScope));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final MemoryAddress rpmtsInitIterator(MemoryAddress ts, int rpmtag, String keyp, long keylen) {
        try (var keypScope = ResourceScope.newConfinedScope()) {
            return (MemoryAddress) Lazy.RPM.rpmtsInitIterator.invoke(
                    ts,
                    rpmtag,
                    toCStringAddress(keyp, keypScope),
                    keylen);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final MemoryAddress rpmdbFreeIterator(MemoryAddress mi) {
        try {
            return (MemoryAddress) Lazy.RPM.rpmdbFreeIterator.invoke(mi);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final MemoryAddress rpmdbNextIterator(MemoryAddress mi) {
        try {
            return (MemoryAddress) Lazy.RPM.rpmdbNextIterator.invoke(mi);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final int rpmReadPackageFile(MemoryAddress ts, MemoryAddress fd, MemoryAddress fn, MemoryAddress hdr) {
        try {
            return (int) Lazy.RPM.rpmReadPackageFile.invoke(ts, fd, fn, hdr);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final int rpmReadConfigFiles(String file, String target) {
        try (var fileScope = ResourceScope.newConfinedScope();
             var targetScope = ResourceScope.newConfinedScope()) {
            return (int) Lazy.RPM.rpmReadConfigFiles.invoke(
                    toCStringAddress(file, fileScope),
                    toCStringAddress(target, targetScope));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final MemoryAddress headerFree(MemoryAddress hdr) {
        try {
            return (MemoryAddress) Lazy.RPM.headerFree.invoke(hdr);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final int headerGet(MemoryAddress hdr, int tag, MemoryAddress td, int flags) {
        try {
            return (int) Lazy.RPM.headerGet.invoke(hdr, tag, td, flags);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final String headerGetString(MemoryAddress hdr, int tag) {
        try {
            return toJavaString((MemoryAddress) Lazy.RPM.headerGetString.invoke(hdr, tag));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final long headerGetNumber(MemoryAddress hdr, int tag) {
        try {
            return (long) Lazy.RPM.headerGetNumber.invoke(hdr, tag);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final MemoryAddress rpmtdNew() {
        try {
            return (MemoryAddress) Lazy.RPM.rpmtdNew.invoke();
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final MemoryAddress rpmtdFree(MemoryAddress td) {
        try {
            return (MemoryAddress) Lazy.RPM.rpmtdFree.invoke(td);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final void rpmtdFreeData(MemoryAddress td) {
        try {
            Lazy.RPM.rpmtdFreeData.invoke(td);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final int rpmtdCount(MemoryAddress td) {
        try {
            return (int) Lazy.RPM.rpmtdCount.invoke(td);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final int rpmtdNext(MemoryAddress td) {
        try {
            return (int) Lazy.RPM.rpmtdNext.invoke(td);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final String rpmtdGetString(MemoryAddress td) {
        try {
            return toJavaString((MemoryAddress) Lazy.RPM.rpmtdGetString.invoke(td));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final MemoryAddress Fopen(String filepath, String mode) {
        try (var filepathScope = ResourceScope.newConfinedScope();
             var modeScope = ResourceScope.newConfinedScope()) {
            return (MemoryAddress) LazyIO.RPMIO.Fopen.invoke(
                    toCStringAddress(filepath, filepathScope),
                    toCStringAddress(mode, modeScope));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final void Fclose(MemoryAddress ts) {
        try {
            LazyIO.RPMIO.Fclose.invoke(ts);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final long Ftell(MemoryAddress fd) {
        try {
            return (long) LazyIO.RPMIO.Ftell.invoke(fd);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final int Ferror(MemoryAddress fd) {
        try {
            return (int) LazyIO.RPMIO.Ferror.invoke(fd);
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    static final String Fstrerror(MemoryAddress fd) {
        try {
            return toJavaString((MemoryAddress) LazyIO.RPMIO.Fstrerror.invoke(fd));
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }
}
