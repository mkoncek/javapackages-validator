Name:           duplicate-file1
Version:        1
Release:        1
Summary:        Multiple RPMs containing the same file path
License:        CC0
BuildArch:      noarch

%description
%{summary}.

%prep

%build

%install
touch '%{buildroot}/><'

%files
/><

%changelog
