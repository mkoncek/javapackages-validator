Name:           javadoc-arch-noarch
Version:        1
Release:        1
Summary:        Archful RPM that contains a noarch javadoc subpackage
License:        CC0

%description
%{summary}.

%package javadoc
BuildArch:      noarch
Summary:        Javadocs for %{name}

%description javadoc
%{summary}.

%prep

%build

%install
mkdir -p '%{buildroot}/usr/share/javadoc'

%files javadoc
/usr/share/javadoc

%changelog
