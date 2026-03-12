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

        // 3. SAC (Strict Avalanche Criterion) 측정
        // 람다식을 사용하여 AvalancheAnalyzer 내부에서 외부 바이너리(choienc)를 실행하게 함
        double sacScore = avalanche.analyzeSAC(inputData -> {
            try {
                return runExternalEncryptor(inputData, new String(key, java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("SAC Error: " + e.getMessage());
                return new byte[inputData.length];
            }
        }, Arrays.copyOf(plain, 16)); // 평문의 앞 16바이트로 테스트

        // 4. Spectral (DFT) 분석
        double spectralScore = spectral.analyzeSpectral(encrypted);

        // 5. 결과 출력 (새 항목 포함)
        System.out.printf("1. Shannon Entropy: %.4f bits (Ideal: 8.0)\n", entropy);
        System.out.printf("2. Linear/Arithmetic Correlation: %.2f/100\n", arithmeticScore);
        System.out.printf("3. SAC (Strict Avalanche): %.2f/100\n", sacScore); // 추가
        System.out.printf("4. Spectral (DFT) Analysis: %.2f/100\n", spectralScore); // 추가
        System.out.printf("5. Kasiski Pattern Analysis: %.2f/100\n", kasiskiScore);
        System.out.printf("6. Block Structural Analysis (ECB): %.2f/100\n", blockScore);

        // 6. 전체 점수 계산 (6개 항목 평균)
        double totalScore = (Math.min(100, (entropy / 8.0) * 100) +
                            arithmeticScore + sacScore + spectralScore +
                            kasiskiScore + blockScore) / 6.0;

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
        // 3 길이의 시퀀스가 반복되는지 검사합니다.
	// 만약 반복성이 있다면 점수가 나빠집니다.
	// 이상적으로 암호화되었다면 완전히 무작위처럼 보여야 합니다.
        Map<String, List<Integer>> sequences = new HashMap<>();
        int seqLen = 3;
        for (int i = 0; i <= data.length - seqLen; i++) {
            String seq = String.format("%02x%02x%02x", data[i], data[i+1], data[i+2]);
            sequences.computeIfAbsent(seq, k -> new ArrayList<>()).add(i);
        }

        int repeatedCount = 0;
        for (List<Integer> positions : sequences.values()) {
            if (positions.size() > 1) {
                repeatedCount++;
            }
        }

        // 데이터 크기와 연관된 반복 패턴 등이 있으면 확실한 감점 요소가 됩니다.
        double penalty = (double) repeatedCount / (data.length / seqLen) * 200;
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
