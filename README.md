# 🔐 Encrymania

**Encrymania**는 암호화된 데이터의 보안 강도를 자동으로 평가하는 **암호 분석(Cryptanalysis) 도구**입니다.

6가지 분석 지표를 통해 암호화 알고리즘의 품질을 0~100점으로 채점하며, CHOI 암호를 비롯한 다양한 암호 시스템의 보안성을 검증할 수 있습니다.

## 📊 분석 항목

| # | 분석 항목 | 설명 | 이상적인 값 |
|---|----------|------|-----------|
| 1 | **Shannon Entropy** | 암호문의 바이트 수준 엔트로피를 측정합니다 | 8.0 bits (완전 무작위) |
| 2 | **Linear/Arithmetic Correlation** | 평문·암호문·키 사이의 수학적 상관관계(XOR, ADD)를 탐지합니다 | 100점 (상관관계 없음) |
| 3 | **SAC (Strict Avalanche Criterion)** | 입력 1비트 변경 시 출력이 얼마나 변하는지 측정합니다 | 100점 (확률 0.5) |
| 4 | **Spectral (DFT) Analysis** | FFT를 통해 암호문의 주기성·패턴을 탐지합니다 | 100점 (주기성 없음) |
| 5 | **Kasiski Pattern Analysis** | 반복되는 바이트 시퀀스를 검색하여 패턴을 탐지합니다 | 100점 (반복 없음) |
| 6 | **Block Structural Analysis (ECB)** | 16바이트 블록의 중복 여부로 ECB 모드를 탐지합니다 | 100점 (중복 없음) |

## 🏷️ 보안 등급

| 점수 | 등급 | 의미 |
|------|------|------|
| 85 이상 | **STRONG** | 현대적이고 안전한 알고리즘으로 판단 |
| 60 ~ 84 | **MODERATE** | 추가적인 보안 감사가 필요 |
| 30 ~ 59 | **WEAK** | 난독화 수준에 불과 |
| 30 미만 | **CRITICAL** | 깨진/장난감 수준의 암호 |

## 🛠️ 요구 사항

- **Java** 21 이상
- **Apache Maven** 3.6 이상
- (선택) CHOI 암호 바이너리 — [choicrypt](https://github.com/gg582/choicrypt)

## 🚀 빌드

```bash
mvn clean compile
```

## 📖 사용법

### 1. 샘플 데이터 생성

```bash
mvn exec:java -Dexec.mainClass="com.encrymania.SampleDataGenerator"
```

`data/` 디렉토리에 아래 파일이 생성됩니다:

| 파일 | 설명 |
|------|------|
| `data/key.bin` | 16바이트 랜덤 키 |
| `data/plain.txt` | 반복 패턴의 평문 |
| `data/encrypted_weak.bin` | XOR 방식의 약한 암호문 |

### 2. 암호 분석 실행

```bash
mvn exec:java -Dexec.mainClass="com.encrymania.CryptanalysisTool" \
    -Dexec.args="--key <키파일_또는_문자열> --encrypted <암호문파일> --plain <평문파일>"
```

**예시:**

```bash
mvn exec:java -Dexec.mainClass="com.encrymania.CryptanalysisTool" \
    -Dexec.args="--key data/key.bin --encrypted data/encrypted_weak.bin --plain data/plain.txt"
```

### 3. CHOI 암호 통합 테스트

[choicrypt](https://github.com/gg582/choicrypt) 저장소를 클론한 뒤 통합 테스트를 실행할 수 있습니다:

```bash
git clone https://github.com/gg582/choicrypt
bash test.sh
```

## 📂 프로젝트 구조

```
encrymania/
├── pom.xml                          # Maven 빌드 설정
├── test.sh                          # CHOI 암호 통합 테스트 스크립트
├── src/
│   └── main/java/com/encrymania/
│       ├── CryptanalysisTool.java   # 메인 분석 엔진 (6개 지표 통합)
│       ├── AvalancheAnalyzer.java   # SAC(눈사태 기준) 분석기
│       ├── SpectralAnalyzer.java    # DFT 기반 스펙트럼 분석기
│       └── SampleDataGenerator.java # 테스트용 샘플 데이터 생성기
└── README.md
```

## 📄 출력 예시

```
========================================
    CRYPTANALYSIS REPORT
========================================
1. Shannon Entropy: 3.9012 bits (Ideal: 8.0)
2. Linear/Arithmetic Correlation: 0.00/100
3. SAC (Strict Avalanche): 45.30/100
4. Spectral (DFT) Analysis: 72.50/100
5. Kasiski Pattern Analysis: 12.40/100
6. Block Structural Analysis (ECB): 35.20/100
----------------------------------------
Overall Security Score: 27.57 / 100
Final Verdict: CRITICAL (Broken/Toy Cipher)
========================================
```

## 📜 라이선스

이 프로젝트는 학술·연구 목적으로 제작되었습니다.
