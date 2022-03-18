/*-
 * Copyright (c) 2016 Red Hat, Inc.
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

import static org.fedoraproject.javadeptools.rpm.Rpm.RPMTAG_ARCH;
import static org.fedoraproject.javadeptools.rpm.Rpm.RPMTAG_EPOCH;
import static org.fedoraproject.javadeptools.rpm.Rpm.RPMTAG_NAME;
import static org.fedoraproject.javadeptools.rpm.Rpm.RPMTAG_RELEASE;
import static org.fedoraproject.javadeptools.rpm.Rpm.RPMTAG_VERSION;
import static org.fedoraproject.javadeptools.rpm.Rpm.headerGetNumber;
import static org.fedoraproject.javadeptools.rpm.Rpm.headerGetString;

import com.sun.jna.Pointer;

/**
 * @author Mikolaj Izdebski
 */
public class NEVRA {
    private final String name;
    private final int epoch;
    private final String version;
    private final String release;
    private final String arch;
    private final String nevra;

    NEVRA(Pointer h) {
        name = headerGetString(h, RPMTAG_NAME);
        epoch = (int) headerGetNumber(h, RPMTAG_EPOCH);
        version = headerGetString(h, RPMTAG_VERSION);
        release = headerGetString(h, RPMTAG_RELEASE);
        arch = headerGetString(h, RPMTAG_ARCH);

        StringBuilder sb = new StringBuilder();
        sb.append(name).append('-');
        if (epoch > 0)
            sb.append(epoch + ":");
        sb.append(version).append('-').append(release);
        sb.append('.').append(arch);
        nevra = sb.toString();
    }

    public String getName() {
        return name;
    }

    public int getEpoch() {
        return epoch;
    }

    public String getVersion() {
        return version;
    }

    public String getRelease() {
        return release;
    }

    public String getArch() {
        return arch;
    }

    @Override
    public String toString() {
        return nevra;
    }

    @Override
    public int hashCode() {
        return nevra.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NEVRA && ((NEVRA) obj).nevra.equals(nevra);
    }
}