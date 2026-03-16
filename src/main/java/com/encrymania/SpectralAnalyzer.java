package com.encrymania;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.*;

public class SpectralAnalyzer {

    public double analyzeSpectral(byte[] encryptedData) {
        try {
            // 분석에 사용할 데이터 크기를 1MB(2^20 bits)로 고정하여 안정성 확보
            // 1MB = 131,072 bytes
            int targetBytes = 131072;
            if (encryptedData.length < targetBytes) {
                // 데이터가 부족하면 현재 가진 크기 중 가장 큰 2의 거듭제곱 비트만큼 사용
                int totalBits = encryptedData.length * 8;
                targetBytes = Integer.highestOneBit(totalBits) / 8;
            }

            if (targetBytes < 128) return 0;

            int n = targetBytes * 8; // 비트 수 (반드시 2의 거듭제곱)
            double[] x = new double[n];

            for (int i = 0; i < n; i++) {
                int byteIdx = i / 8;
                int bitShift = 7 - (i % 8);
                int bit = (encryptedData[byteIdx] >> bitShift) & 1;
                x[i] = 2.0 * bit - 1.0;
            }

            FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
            Complex[] out = transformer.transform(x, TransformType.FORWARD);

            int halfN = n / 2;
            double threshold = Math.sqrt(Math.log(1.0 / 0.05) * n);
            int n0 = (int) (0.95 * halfN);
            int actualN1 = 0;

            for (int i = 0; i < halfN; i++) {
                if (out[i].abs() < threshold) {
                    actualN1++;
                }
            }

            double d = (actualN1 - n0) / Math.sqrt(n * 0.95 * 0.05 / 4.0);
            double pValue = Math.exp(-d * d / 2.0);

            if (Double.isNaN(pValue)) return 0;
            return convertPValueToScore(pValue);

        } catch (Exception e) {
            // 터미널에 에러 메시지를 직접 출력하여 원인 파악
            System.err.println("\n[!] DFT Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return 0;
        }
    }

    private double convertPValueToScore(double pValue) {
        if (pValue < 0.01) return pValue * 100;
        return Math.min(100, 50 + (pValue * 50));
    }
}
