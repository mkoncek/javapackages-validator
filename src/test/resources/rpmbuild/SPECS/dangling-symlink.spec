Name:           dangling-symlink
Version:        1
Release:        1
Summary:        RPM that contains an unresolved symbolic link
License:        CC0
BuildArch:      noarch

%description
%{summary}.

%prep

%build

%install
ln -s /# '%{buildroot}/->'

%files
/->

%changelog
