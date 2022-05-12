Name:           valid-symlink
Version:        1
Release:        1
Summary:        RPM that contains a valid symbolic link
License:        CC0
BuildArch:      noarch

%description
%{summary}.

%prep

%build

%install
ln -s /bin '%{buildroot}/->'

%files
/->

%changelog
