Name:           javadoc-noarch-noarch
Version:        1
Release:        1
Summary:        Noarch RPM that contains a noarch javadoc subpackage
BuildArch:      noarch
License:        CC0

%description
%{summary}.

%package javadoc
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
