Name:           javadoc-arch-arch
Version:        1
Release:        1
Summary:        Archful RPM that contains archful javadoc subpackage
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
