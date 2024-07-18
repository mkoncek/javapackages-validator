Name:           jpms-automatic
Version:        1
Release:        1
Summary:        RPM that contains a JAR with Automatic-Module-Name
License:        CC0
BuildArch:      noarch
Provides:       jpms(foo.bar)
%description
%{summary}.
%prep
%build
rm -rf %{name}
mkdir -p %{name}
echo "Automatic-Module-Name: foo.bar" >> %{name}/MANIFEST.MF
jar -c -m %{name}/MANIFEST.MF -f %{name}/%{name}.jar

%install
install -m 644 -D %{name}/%{name}.jar %{buildroot}/usr/share/java/%{name}.jar

%files
/usr/share/java/%{name}.jar

%changelog
