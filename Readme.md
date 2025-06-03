# 📱 HelloSTK3 - SIM Toolkit Applet for JSON Reporting via BIP

## Description

This project implements a JavaCard applet for SIM Toolkit that collects information from the SIM/UICC card (such as ICCID, IMEI, MCC, and MNC) and sends it in JSON format to a remote server using Bearer Independent Protocol (BIP).

The applet leverages:
- **SIM Toolkit (STK)** for menu events and event downloads.
- **BIP** to open an IP channel and send HTTP POST data.
- **JsonUtil** to efficiently build the JSON body.
- **UICC access** and **toolkit handlers** to operate according to Release 6 best practices.


---

## 📦 Project Structure

This project is structured following a modular organization, with specific folders for source code, libraries, SDKs, and build artifacts.

- **`src/`** — Contains all the Java source code organized into packages (`core`, `bip`, `uicc`, `util`).
- **`lib/` and `exp/`** — Contain the UICC and STK JavaCard libraries (compiled JAR and EXP files) extracted from the official [ETSI TS 102 241 v17.4.0 specification](https://www.etsi.org/deliver/etsi_ts/102200_102299/102241/17.04.00_60/ts_102241v170400p.pdf).
  - These libraries were downloaded directly from the official ETSI ZIP package:  
    [https://www.etsi.org/deliver/etsi_ts/102200_102299/102241/17.04.00_60/ts_102241v170400p0.zip](https://www.etsi.org/deliver/etsi_ts/102200_102299/102241/17.04.00_60/ts_102241v170400p0.zip)
- **`oracle_javacard_sdks/`** — Contains multiple versions of the official Oracle JavaCard SDKs.
  - These SDKs were obtained from the public repository:  
    [https://github.com/martinpaljak/oracle_javacard_sdks](https://github.com/martinpaljak/oracle_javacard_sdks/tree/282c7f510d704045ad12c8a6525287e7ab7b82d5)

These resources ensure that the project fully complies with the official JavaCard, UICC, and SIM Toolkit standards, allowing for maximum interoperability and reproducibility.

| Component                   | Description |
|------------------------------|-------------|
| `STKHandler.java`        | Handles STK events, builds the JSON, and initiates HTTP communication. |
| `UICCInfoProvider.java`  | Retrieves ICCID, IMEI, MCC, and MNC information from the UICC. |
| `BIPManager.java`         | Manages BIP channel operations and data transmission. |
| `JsonUtil.java`          | Utility class for building JSON payloads efficiently. |
| `DiagUtil.java`          | Utility class for diagnostics and error logging. |

---

## 🚀 Deployment Guide

This section describes the necessary steps to build and deploy the HelloSTK3 applet onto a UICC/SIM card.

---

### 1. Installing dependencies
```bash
sudo apt update && sudo apt install ant openjdk-8-jdk
```
### 2. Building the CAP File

The project uses [ant-javacard](https://github.com/martinpaljak/ant-javacard) for building the JavaCard CAP file.

To generate the `.cap` file:

1. Make sure the JavaCard SDKs are correctly placed:
  - JavaCard 2.2.1 SDK for target compatibility
  - JavaCard 3.0.5u3 SDK for compiling
2. Open a terminal inside the project directory.
3. Run the following command:

```bash
ant
```

If the build is successful, you will find the compiled `HelloSTK3.cap` file inside the `/build/` directory.

> ⚡ Deployment references: Part of the deployment process follows the guidelines described in the [Osmocom SIM Toolkit Wiki](https://osmocom.org/projects/sim-toolkit/wiki).

---

### 3. Deploying the CAP File to the SIM Card

The applet deployment is carried out using the [**sim-tools**](https://github.com/ramosaurio/sim-tools) repository, which contains a customized version of `shadysim` tailored for advanced SIM Toolkit development and secure OTA scenarios.

To install the **HelloSTK3** applet on a compatible SIM card, use the following command:

```
python3 shadysim.py \
  --pcsc \
  -l /path/to/your/build/HelloSTK3.cap \
  -i /path/to/your/build/HelloSTK3.cap \
  --enable-sim-toolkit \
  --module-aid d0:70:02:CA:44:90:01:01 \
  --instance-aid d0:70:02:CA:44:90:01:01 \
  --nonvolatile-memory-required 0100 \
  --volatile-memory-for-install 0100 \
  --max-menu-entry-text 15 \
  --max-menu-entries 05 \
  --max-bip-channel 4 \
  --access-domain 00 \
  --max-timers 2 \
  --kic <YOUR_KIC_HEX> \
  --kid <YOUR_KID_HEX>
  
```

> ⚙️ Replace the placeholders (`<YOUR_MODULE_AID_HEX>`, `<YOUR_INSTANCE_AID_HEX>`, `<YOUR_KIC_HEX>`, `<YOUR_KID_HEX>`) with the actual values defined during CAP file generation or provisioning.

This command handles both loading and installation of the CAP file, and automatically configures key STK-related parameters such as BIP support, menu entries, timers, and secure channel credentials (KIC/KID) for OTA operations.

**Test Environment:**
- **SIM Card Model**: Sysmocom ISIM-SJA5-9FV
- **Card Capabilities**:
  - JavaCard 2.2.1 compliant
  - SIM Toolkit (STK) and Bearer Independent Protocol (BIP) supported
  - Compatible with secure OTA commands via KIC/KID

Once installed, the HelloSTK3 applet is activated and ready to interact with the SIM Toolkit runtime environment, including proactive commands and BIP sessions.

---

### 4. Post-deployment

After deployment, the following applet features are available:

- STK menu entry dynamically inserted
- SIM local information retrieval (ICCID, IMEI, MCC, MNC)
- JSON payload construction
- HTTP POST communication via BIP channel

The applet is fully functional and ready to operate within compatible mobile devices or card readers supporting UICC-based applications.

---

## 🛠️ Technologies and APIs Used

- **JavaCard API 2.2.1** (`javacard.framework`)
- **UICC API** (`uicc.toolkit`, `uicc.system`, `uicc.access`)
- **ETSI Specifications**:
  - TS 102 221 — UICC-Terminal Interface
  - TS 102 223 — Card Application Toolkit
  - TS 102 241 — JavaCard API for UICC
- **3GPP Specifications**:
  - TS 31.101 — UICC-terminal physical and logical interface
  - TS 31.102 — USIM application characteristics
  - TS 31.111 — USIM Application Toolkit (USAT)
- **GlobalPlatform 2.1.1** — JavaCard applet management framework
- **Apache Ant** — Build system used to compile and package the CAP file
- **JavaCard Converter** — JavaCard SDK tool (`converter`) used for CAP file generation
- **SIM Card Used**: [Sysmocom sysmoISIM-SJA5-9FV](https://sysmocom.de/products/sim/sysmoisim-sja5/index.html) (JavaCard 2.2.1 compatible, BIP and STK capabilities)
- **Smart Card Readers**:
  - NOX brand smartcard reader
  - [HID Omnikey 6121 Mobile Smart Card Reader](https://www.hidglobal.com/es/products/omnikey-6121-mobile-smart-card-reader)

---
## 💻 Hardware Requirements

The following hardware is required to successfully deploy and operate the HelloSTK3 applet:

- **JavaCard 2.2.1 compatible SIM card**  
  (e.g., [Sysmocom sysmoISIM-SJA5-9FV](https://sysmocom.de/products/sim/sysmoisim-sja5/index.html))
- **Smart card reader compatible with ISO/IEC 7816 standards**:
  - Must support **T=0** and **T=1** communication protocols
  - Must be **PC/SC** compliant for integration with standard smart card libraries
- **Examples of supported card readers**:
  - NOX smartcard reader
  - [HID Omnikey 6121 Mobile Smart Card Reader](https://www.hidglobal.com/es/products/omnikey-6121-mobile-smart-card-reader)
- **Computer with a USB interface** to connect the smart card reader
- **Operating system with smart card support** (Windows, Linux, or macOS)

Optional but recommended:
- Administrative privileges on the system to manage smart card access and reader drivers

---

## 📚 References and Credits

The design and development of this applet are based primarily on the following sources:

- **Trusted Connectivity Alliance** (formerly SIM Alliance):
  - [*Interoperability Stepping Stones Release 6*](https://trustedconnectivityalliance.org/wp-content/uploads/2020/01/StepStonesRelease6_v100.pdf)
- **ETSI (European Telecommunications Standards Institute)**:
  - [TS 102 221 — UICC-Terminal interface](https://www.etsi.org/deliver/etsi_ts/102200_102299/102221/15.06.00_60/ts_102221v150600p.pdf)
  - [TS 102 223 — Card Application Toolkit](https://www.etsi.org/deliver/etsi_ts/102200_102299/102223/16.01.00_60/ts_102223v160100p.pdf)
  - [TS 102 241 — JavaCard API for UICC](https://www.etsi.org/deliver/etsi_ts/102200_102299/102241/12.00.00_60/ts_102241v120000p.pdf)
- **3GPP (3rd Generation Partnership Project)**:
  - [TS 31.101 — UICC-terminal physical/logical interface](https://portal.3gpp.org/desktopmodules/Specifications/SpecificationDetails.aspx?specificationId=1525)
  - [TS 31.102 — USIM application characteristics](https://portal.3gpp.org/desktopmodules/Specifications/SpecificationDetails.aspx?specificationId=1526)
  - [TS 31.111 — USIM Application Toolkit (USAT)](https://portal.3gpp.org/desktopmodules/Specifications/SpecificationDetails.aspx?specificationId=1586)
- **Sun Microsystems**:
  - *Java Card 2.2.1 VM Specification* (archived)
  - *Java Card 2.2.1 API Specification* (archived)
  - *Java Card Applet Developer’s Guide* (archived)
- **NTT Communications SDPF Developer Portal**:
  - [https://sdpf.ntt.com/](https://sdpf.ntt.com/) — Various technical materials were used as guidance, especially related to SIM Toolkit command handling, BIP communication setup, and JavaCard applet development practices.
- **HelloSTK2 Project** by mrlnc:
  - [https://github.com/mrlnc/HelloSTK2](https://github.com/mrlnc/HelloSTK2) — A JavaCard applet project that inspired the overall architecture and event handling design.
- **Osmocom HelloSTK Example** (official Osmocom community example):
  - [https://gitea.osmocom.org/sim-card/hello-stk/](https://gitea.osmocom.org/sim-card/hello-stk/) — Used for reference on SIM Toolkit menu creation and minimal applet structure.
- **Osmocom SIM Toolkit Build and Deployment Guide**:
  - [https://osmocom.org/projects/sim-toolkit/wiki](https://osmocom.org/projects/sim-toolkit/wiki) — Used as a reference for CAP file building and SIM applet deployment procedures.
- **ETSI TS 102 241 v17.4.0**:
  - [JavaCard API for UICC Applications](https://www.etsi.org/deliver/etsi_ts/102200_102299/102241/17.04.00_60/ts_102241v170400p.pdf) — The UICC and STK JavaCard libraries used in the project (`exp/` and `lib/` folders) were extracted from this official specification.
- **sim-tools (shadysim fork)**:
  - [https://github.com/ramosaurio/sim-tools](https://github.com/ramosaurio/sim-tools) — Custom deployment tool used to load and install the HelloSTK3 CAP file onto JavaCard-based SIMs, with full support for STK, BIP, and secure OTA parameters.

> **Note:** All technical concepts related to UICC architecture, event management, security, and APDU flow have been adapted according to the interoperability recommendations from the sources cited above.
---

## ⚖️ License and Limitations

This project was developed for educational and research purposes as part of a Final Degree Project (TFG) in Computer Engineering.

- **License**: The source code may be used and modified for non-commercial purposes, provided that the original authorship and cited references are acknowledged.
- **Limitations**:
  - The applet assumes that the SIM/UICC supports BIP and open IP channels.
  - No HTTP traffic encryption (TLS) is included in this version.
  - The server must accept unauthenticated HTTP POST connections.
  - Advanced network error management and channel recovery are not implemented.

For production environments, it is strongly recommended to add SSL/TLS encryption, strong authentication, and robust network error recovery mechanisms.

---
## 🛠️ Troubleshooting and Important Considerations

During the development and building process, two major constraints were encountered, specific to JavaCard 2.2.1 and the available CAP file converters:

### 1. Multidimensional Arrays Not Supported

JavaCard 2.2.1 and its CAP converter do **not support multidimensional arrays** (e.g., `byte[][]`) due to limitations in the JavaCard virtual machine (JCVM) bytecode set.

Attempting to use `byte[][]` leads to CAP conversion errors such as:

- `unsupported parameter type multidimensional array`
- `unsupported field type multidimensional array`
- `unsupported bytecode anewarray`

**Solution applied**:
- Flattened structures were used instead of multidimensional arrays.
- For example, `byte[][] keys` was replaced with a single `byte[] keys` and auxiliary `keyOffsets` and `keyLengths` arrays to simulate structured access.

This approach ensures full compatibility with JavaCard 2.2.1 without generating unsupported bytecodes.

### 2. Limited Package Management in CAP Conversion

The `ant-javacard` build tool used in this project does not fully support advanced CAP packaging features like multiple internal packages (`<package>` tags inside `<cap>`). It expects all source code to belong to a single logical package hierarchy without separate `.exp` files for internal packages.

Attempting to declare multiple packages separately led to errors like:

- `cap doesn't support the nested "package" element`
- `export file util.exp of package ... not found`

**Solution applied**:
- All source code was compiled together as a single module under a unified base package (`com.brownfields.github.hellostk3`), with logical separation managed through folder structure only (e.g., `core`, `bip`, `uicc`, `util`).
- No additional `.exp` files were generated or required.

This structure allows seamless CAP generation while maintaining clean code organization internally.

---

By following these approaches, the project remains fully buildable, deployable, and interoperable with JavaCard 2.2.1 platforms while preserving good modular design practices.

## 📜 Changelog

### v1.0 — Initial Release

- Developed HelloSTK3 JavaCard applet for SIM Toolkit interaction and BIP communication.
- Implemented dynamic STK menu entry creation.
- Integrated UICC information retrieval (ICCID, IMEI, MCC, MNC) from SIM card.
- Designed JSON payload construction optimized for JavaCard 2.2.1 resource constraints.
- Established HTTP POST transmission over BIP channel.
- Organized project into modular components:
  - Core STK event handler
  - BIP channel manager
  - UICC information provider
  - JSON construction utilities
  - Diagnostic utilities
- Full compliance with JavaCard 2.2.1, ETSI, 3GPP, and GlobalPlatform specifications.
- Deployment tested successfully on Sysmocom ISIM-SJA5-9FV using smart card readers (NOX and HID Omnikey 6121).

---

## 👨‍💻 Author

- **Name**: Rafael Moreno
- **Email**: rmoreno.morcam@gmail.com
- **Project**: Final Degree Project (TFG) - Computer Engineering

---
