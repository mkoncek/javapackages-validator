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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

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
    static final int RPMVSF_NOMD5HEADER = 1 << 9;
    static final int RPMVSF_NODSAHEADER = 1 << 10;
    static final int RPMVSF_NORSAHEADER = 1 << 11;
    static final int RPMVSF_NOSHA1 = 1 << 16;
    static final int RPMVSF_NOMD5 = 1 << 17;
    static final int RPMVSF_NODSA = 1 << 18;
    static final int RPMVSF_NORSA = 1 << 19;

    static final int RPMTAG_NAME = 1000;
    static final int RPMTAG_VERSION = 1001;
    static final int RPMTAG_RELEASE = 1002;
    static final int RPMTAG_EPOCH = 1003;
    static final int RPMTAG_ARCH = 1022;
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

    private static interface RpmLib extends Library {

        int rpmReadConfigFiles(String file, String target);

        Pointer rpmtsCreate();

        void rpmtsFree(Pointer ts);

        int rpmtsSetRootDir(Pointer ts, String rootDir);

        Pointer rpmtsInitIterator(Pointer ts, int rpmtag, String keyp, long keylen);

        void rpmtsSetVSFlags(Pointer ts, int vsflags);

        Pointer rpmdbNextIterator(Pointer mi);

        void rpmdbFreeIterator(Pointer mi);

        int rpmReadPackageFile(Pointer ts, Pointer fd, Pointer fn, Pointer hdrp);

        void headerFree(Pointer h);

        boolean headerGet(Pointer h, int tag, Pointer td, int flags);

        String headerGetString(Pointer h, int tag);

        long headerGetNumber(Pointer h, int tag);

        int rpmtdCount(Pointer td);

        int rpmtdNext(Pointer td);

        String rpmtdGetString(Pointer td);

        void rpmtdFreeData(Pointer td);
    }

    private static interface RpmIO extends Library {

        Pointer Fopen(String path, String mode);

        void Fclose(Pointer fd);

        long Ftell(Pointer fd);

        boolean Ferror(Pointer fd);

        String Fstrerror(Pointer fd);

    }

    private static class Lazy {
        static final RpmLib RPM = (RpmLib) Native.loadLibrary("rpm", RpmLib.class);
    }

    private static class LazyIO {
        static final RpmIO RPMIO = (RpmIO) Native.loadLibrary("rpmio", RpmIO.class);
    }

    static final Pointer Fopen(String path, String mode) {
        return LazyIO.RPMIO.Fopen(path, mode);
    }

    static final void Fclose(Pointer fd) {
        LazyIO.RPMIO.Fclose(fd);
    }

    static final long Ftell(Pointer fd) {
        return LazyIO.RPMIO.Ftell(fd);
    }

    static final boolean Ferror(Pointer fd) {
        return LazyIO.RPMIO.Ferror(fd);
    }

    static final String Fstrerror(Pointer fd) {
        return LazyIO.RPMIO.Fstrerror(fd);
    }

    static final int rpmReadConfigFiles(String file, String target) {
        return Lazy.RPM.rpmReadConfigFiles(file, target);
    }

    static final Pointer rpmtsCreate() {
        return Lazy.RPM.rpmtsCreate();
    }

    static final void rpmtsFree(Pointer ts) {
        Lazy.RPM.rpmtsFree(ts);
    }

    static final int rpmtsSetRootDir(Pointer ts, String rootDir) {
        return Lazy.RPM.rpmtsSetRootDir(ts, rootDir);
    }

    static final Pointer rpmtsInitIterator(Pointer ts, int rpmtag, String keyp, long keylen) {
        return Lazy.RPM.rpmtsInitIterator(ts, rpmtag, keyp, keylen);
    }

    static final void rpmtsSetVSFlags(Pointer ts, int vsflags) {
        Lazy.RPM.rpmtsSetVSFlags(ts, vsflags);
    }

    static final Pointer rpmdbNextIterator(Pointer mi) {
        return Lazy.RPM.rpmdbNextIterator(mi);
    }

    static final void rpmdbFreeIterator(Pointer mi) {
        Lazy.RPM.rpmdbFreeIterator(mi);
    }

    static final int rpmReadPackageFile(Pointer ts, Pointer fd, Pointer fn, Pointer hdrp) {
        return Lazy.RPM.rpmReadPackageFile(ts, fd, fn, hdrp);
    }

    static final void headerFree(Pointer h) {
        Lazy.RPM.headerFree(h);
    }

    static final boolean headerGet(Pointer h, int tag, Pointer td, int flags) {
        return Lazy.RPM.headerGet(h, tag, td, flags);
    }

    static final String headerGetString(Pointer h, int tag) {
        return Lazy.RPM.headerGetString(h, tag);
    }

    static final long headerGetNumber(Pointer h, int tag) {
        return Lazy.RPM.headerGetNumber(h, tag);
    }

    static final int rpmtdCount(Pointer td) {
        return Lazy.RPM.rpmtdCount(td);
    }

    static final int rpmtdNext(Pointer td) {
        return Lazy.RPM.rpmtdNext(td);
    }

    static final String rpmtdGetString(Pointer td) {
        return Lazy.RPM.rpmtdGetString(td);
    }

    static final void rpmtdFreeData(Pointer td) {
        Lazy.RPM.rpmtdFreeData(td);
    }
}
