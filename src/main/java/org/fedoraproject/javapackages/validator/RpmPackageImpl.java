package org.fedoraproject.javapackages.validator;

import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.config.Nevra;
import org.fedoraproject.javapackages.validator.config.RpmPackage;

public class RpmPackageImpl implements RpmPackage {
    private RpmInfo rpmInfo;

    public RpmPackageImpl(RpmInfo rpmInfo) {
        super();
        this.rpmInfo = rpmInfo;
    }

    @Override
    public String getPackageName() {
        if (isSourceRpm()) {
            return getNevra().getName();
        } else {
            return Common.getPackageName(rpmInfo.getSourceRPM());
        }
    }

    @Override
    public Nevra getNevra() {
        return rpmInfo.getNEVRA();
    }

    @Override
    public boolean isSourceRpm() {
        return rpmInfo.isSourcePackage();
    }
}
