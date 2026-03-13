package com.encrymania;

import java.util.Arrays;

public class AvalancheAnalyzer {

	/**
	 * @param engine 지수귀문도 C 라이브러리와 연결된 암호화 엔진 (JNI 또는 Process 실행기)
	 * @param samplePlain 분석에 사용할 평문 (보통 16바이트 블록 단위)
	 * @return Avalanche 만족도 점수 (0~100, 100에 가까울수록 이상적)
	 */
	public double analyzeAvalanche(EncryptionEngine engine, byte[] samplePlain) {
		int blockSize = samplePlain.length;
		int numBits = blockSize * 8;

		// 각 입력 비트(i)를 뒤집었을 때 출력 비트(j)가 변한 횟수를 기록하는 행렬
		int[][] avalancheMatrix = new int[numBits][numBits];
		int testIterations = 100; // 정밀도를 높이려면 이 횟수를 늘리세요.

		for (int t = 0; t < testIterations; t++) {
			// 매 반복마다 무작위 평문 생성 (다양한 패턴 테스트)
			byte[] plain = generateRandomBlock(blockSize);
			byte[] originalCipher = engine.encrypt(plain);

			for (int i = 0; i < numBits; i++) {
				byte[] flippedPlain = flipBit(plain, i);
				byte[] flippedCipher = engine.encrypt(flippedPlain);

				for (int j = 0; j < numBits; j++) {
					if (isBitDifferent(originalCipher, flippedCipher, j)) {
						avalancheMatrix[i][j]++;
					}
				}
			}
		}

		return calculateAvalancheScore(avalancheMatrix, testIterations);
	}

	// 특정 위치의 비트를 반전시키는 메서드
	private byte[] flipBit(byte[] data, int bitIdx) {
		byte[] copy = data.clone();
		copy[bitIdx / 8] ^= (1 << (7 - (bitIdx % 8)));
		return copy;
	}

	// 두 바이트 배열의 특정 비트가 다른지 확인
	private boolean isBitDifferent(byte[] c1, byte[] c2, int bitIdx) {
		int byteIdx = bitIdx / 8;
		int mask = (1 << (7 - (bitIdx % 8)));
		return (c1[byteIdx] & mask) != (c2[byteIdx] & mask);
	}

	// 최종 Avalanche 점수 계산 (이상적인 확률 0.5와의 편차 측정)
	private double calculateAvalancheScore(int[][] matrix, int iterations) {
		double totalDeviation = 0;
		int numBits = matrix.length;
		double ideal = 0.5;

		for (int i = 0; i < numBits; i++) {
			for (int j = 0; j < numBits; j++) {
				double probability = (double) matrix[i][j] / iterations;
				totalDeviation += Math.abs(probability - ideal);
			}
		}

		// 편차가 0에 가까울수록 100점, 멀어질수록 감점
		double avgDeviation = totalDeviation / (numBits * numBits);
		return Math.max(0, 100 - (avgDeviation * 200));
	}

	private byte[] generateRandomBlock(int size) {
		byte[] b = new byte[size];
		new java.util.Random().nextBytes(b);
		return b;
	}
}
