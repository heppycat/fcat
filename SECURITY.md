# Security policy

fcat opens untrusted files, so parser and rendering bugs can be security bugs. We
take them seriously.

## Supported versions

There is no release yet. Once there is, only the latest release is supported.

## Reporting a vulnerability

**Do not open a public issue.** Use GitHub's private vulnerability reporting:
"Report a vulnerability" under the repository's **Security** tab. The maintainers
will enable it when the repository is published. If it is not available, open an
issue saying *only* that you have a security report and need a private channel,
without any details.

Please include:

- the affected version or commit;
- the device and Android version;
- steps to reproduce;
- a proof-of-concept file if possible. Share it only through the private report.

This is a volunteer project, so there is no guaranteed response time. We aim to
acknowledge reports within a week, and we will credit reporters unless they prefer
otherwise.

## Scope

- **In scope:** crashes or memory corruption from crafted PDFs, data loss,
  unintended file access, anything that breaks the "no network, no data leaves the
  device" promise.
- **Out of scope:** vulnerabilities in the upstream engines (MuPDF, PDFium) or in
  Android itself. Report those upstream. Do tell us if fcat ships an affected
  version.
