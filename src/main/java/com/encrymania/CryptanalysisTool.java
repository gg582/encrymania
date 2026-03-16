package com.encrymania;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CryptanalysisTool {

    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Usage: java CryptanalysisTool --key <keyfile_or_string> --encrypted <encfile> --plain <plainfile>");
            return;
        }

        String keyInput = null, encPath = null, plainPath = null;
        for (int i = 0; i < args.length; i++) {
            if ("--key".equals(args[i])) keyInput = args[++i];
            else if ("--encrypted".equals(args[i])) encPath = args[++i];
            else if ("--plain".equals(args[i])) plainPath = args[++i];
        }

        try {
            byte[] key;
            try {
                key = Files.readAllBytes(Paths.get(keyInput));
            } catch (Exception e) {
                key = keyInput.getBytes(StandardCharsets.UTF_8);
            }

            byte[] encrypted = Files.readAllBytes(Paths.get(encPath));
            byte[] plain = Files.readAllBytes(Paths.get(plainPath));

            analyze(key, encrypted, plain);
        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
        }
    }

private static void analyze(byte[] key, byte[] encrypted, byte[] plain) {
        System.out.println("========================================");
        System.out.println("    CRYPTANALYSIS REPORT                ");
        System.out.println("========================================");

        // 1. 기존 통계 분석
        double entropy = calculateEntropy(encrypted);
        double arithmeticScore = testArithmeticCorrelation(key, encrypted, plain);
        double lengthScore = testLengthPreservation(plain, encrypted);
        double kasiskiScore = testKasiski(encrypted);
        double blockScore = testBlockPatterns(encrypted);

        // 2. 고급 분석 모듈 객체 생성
        AvalancheAnalyzer avalanche = new AvalancheAnalyzer();
        SpectralAnalyzer spectral = new SpectralAnalyzer();

	 double dftScore = spectral.analyzeSpectral(encrypted);

        // 4. 결과 출력 (새 항목 포함)
        System.out.printf("1. 샤논 엔트로피: %.4f bits (Ideal: 8.0)\n", entropy);
        System.out.printf("2. 선형/산술 상관관계: %.2f/100\n", arithmeticScore);
        System.out.printf("3. 스펙트럼 (DFT) 분석: %.2f/100\n", dftScore);
        System.out.printf("4. Kasiski Pattern Analysis: %.2f/100\n", kasiskiScore);
        System.out.printf("5. Block Structural Analysis (ECB): %.2f/100\n", blockScore);

        // 5. 전체 점수 계산 (6개 항목 평균)
        double totalScore = (Math.min(100, (entropy / 8.0) * 100) +
                                arithmeticScore + dftScore +
                            kasiskiScore + blockScore) / 5.0;

        System.out.println("----------------------------------------");
        System.out.printf("Overall Security Score: %.2f / 100\n", totalScore);

        // 7. 최종 판정 (기존 로직)
        String verdict;
        if (totalScore < 30) verdict = "CRITICAL (Broken/Toy Cipher)";
        else if (totalScore < 60) verdict = "WEAK (Obfuscation only)";
        else if (totalScore < 85) verdict = "MODERATE (Needs deeper audit)";
        else verdict = "STRONG (Likely modern algorithm)";

        System.out.println("Final Verdict: " + verdict);
        System.out.println("========================================");
    }

    private static double calculateEntropy(byte[] data) {
        if (data.length == 0) return 0;
        int[] freq = new int[256];
        for (byte b : data) freq[b & 0xFF]++;

        double entropy = 0;
        for (int f : freq) {
            if (f > 0) {
                double p = (double) f / data.length;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    private static double testArithmeticCorrelation(byte[] key, byte[] encrypted, byte[] plain) {
        int len = Math.min(encrypted.length, plain.length);
        if (len == 0 || key.length == 0) return 0;

        int xorMatches = 0;
        int subMatches = 0;

        for (int i = 0; i < len; i++) {
            byte expectedKeyByte = key[i % key.length];
            if ((plain[i] ^ encrypted[i]) == expectedKeyByte) xorMatches++;
            if (((encrypted[i] - plain[i]) & 0xFF) == (expectedKeyByte & 0xFF)) subMatches++;
        }

        double xorRatio = (double) xorMatches / len;
        double subRatio = (double) subMatches / len;

        // XOR이나 ADD와 정확히 일치하면 이것의 점수는 0이 될 것
        double maxCorrelation = Math.max(xorRatio, subRatio);
        return Math.max(0, 100 - (maxCorrelation * 100));
    }

    private static double testDiffusion(byte[] plain, byte[] encrypted) {
        // 디퓨전의 경우에는 패딩/IV 등이 있어서 원본과 길이가 다를때 100점입니다.
        if (plain.length != encrypted.length) return 100;

        // 단순히 각 바이트를 암호화한 경우에는 감점합니다.
        return 20;
    }

    private static double testLengthPreservation(byte[] plain, byte[] encrypted) {
        if (plain.length == encrypted.length) {
            // 패딩이 없을 경우에는 당연히 길이 보존입니다. 이것은 원본 데이터의 길이를 쉽게 유추할 수 있어서 명확한 감점 사항입니다.
            return 0;
        }
        return 100;
    }

		private static double testKasiski(byte[] data) {
		    // 데이터가 너무 적으면 분석의 의미가 없으므로 100점 반환
		    if (data.length < 100) return 100;

		    // 대용량 데이터(10M+) 분석을 위해 패턴 탐색 길이를 4바이트로 상향
		    // (256^4 = 약 42억 가짓수로, 무작위 데이터에서의 우연한 중복 방지)
		    int seqLen = 4;

		    // 패턴 출현 횟수 기록 (Integer를 키로 사용하여 메모리 효율 극대화)
		    Map<Integer, Integer> counts = new HashMap<>();

		    // 슬라이딩 윈도우 방식으로 전체 데이터 스캔
		    for (int i = 0; i <= data.length - seqLen; i++) {
		        // 비트 시프트를 이용해 4바이트 정수(int) 패턴 생성
		        // String.format 방식보다 수십 배 빠르고 GC 부하가 거의 없음
		        int pattern = ((data[i] & 0xFF) << 24) |
		                      ((data[i+1] & 0xFF) << 16) |
		                      ((data[i+2] & 0xFF) << 8)  |
		                      (data[i+3] & 0xFF);

		        counts.put(pattern, counts.getOrDefault(pattern, 0) + 1);
		    }

		    // 통계적 중복 횟수 집계
		    int repeatedCount = 0;
		    for (int count : counts.values()) {
		        if (count > 1) {
		            // 동일 패턴이 n번 나타나면 n-1번의 중복으로 간주
		            repeatedCount += (count - 1);
		        }
		    }

		    // 페널티 계산 로직 (대용량 정규화 반영)
		    // 10M 데이터에서 4바이트 패턴이 발견되는 것은 통계적으로 매우 드문 일이므로,
		    // 발견된 중복은 지수귀문도 구조나 CTR 모드 상의 주기적 결함일 확률이 높음.
		    // 현실적인 감점 계수(50)를 적용하여 정밀 분석 수행.
		    double penalty = (double) repeatedCount / (data.length / (double) seqLen) * 50;

		    // 최종 점수 계산 (최솟값 0점 보장)
		    return Math.max(0, 100 - penalty);
		}

    private static double testBlockPatterns(byte[] encrypted) {
        int blockSize = 16;
        if (encrypted.length < blockSize * 2) return 100;

        Map<String, Integer> counts = new HashMap<>();
        int repeats = 0;
        for (int i = 0; i <= encrypted.length - blockSize; i += blockSize) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < blockSize; j++) sb.append(String.format("%02x", encrypted[i+j]));
            String block = sb.toString();
            counts.put(block, counts.getOrDefault(block, 0) + 1);
        }

        for (int count : counts.values()) if (count > 1) repeats += (count - 1);

        double penalty = (double) repeats / (encrypted.length / blockSize) * 100;
        return Math.max(0, 100 - penalty);
    }
}
