import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.*;

public class SpectralAnalyzer {

    /**
     * @param encryptedData 분석할 암호화된 바이너리 데이터 (최소 1MB 권장)
     * @return Spectral Test 통과 점수 (0~100)
     */
    public double analyzeSpectral(byte[] encryptedData) {
        int n = encryptedData.length * 8; // 전체 비트 수
        double[] x = new double[n];

        // 1. 데이터를 비트 스트림(-1, +1)으로 변환
        // 0은 -1로, 1은 +1로 매핑하여 직류 성분 제거
        for (int i = 0; i < encryptedData.length; i++) {
            for (int j = 0; j < 8; j++) {
                int bit = (encryptedData[i] >> (7 - j)) & 1;
                x[i * 8 + j] = 2 * bit - 1;
            }
        }

        // 2. FFT(Fast Fourier Transform) 수행
        // 입력 크기가 2의 거듭제곱이어야 하므로 적절히 조정 필요 (여기선 단순화)
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] out = transformer.transform(x, TransformType.FORWARD);

        // 3. 진폭(Magnitude) 계산 및 임계값(Threshold) 설정
        double[] magnitudes = new double[n / 2];
        double threshold = Math.sqrt(Math.log(1 / 0.05) * n); // 95% 신뢰구간 임계값
        int n0 = (int) (0.95 * (n / 2.0)); // 이론적 기대치 (임계값 이하의 피크 수)
        int actualN1 = 0; // 실제 임계값 이하인 피크 수 계산

        for (int i = 0; i < n / 2; i++) {
            magnitudes[i] = out[i].abs();
            if (magnitudes[i] < threshold) {
                actualN1++;
            }
        }

        // 4. P-Value 도출 (통계적 유의성 검정)
        // d 값이 0에 가까울수록 이상적이며, 멀어질수록 주기성이 강하다는 증거
        double d = (actualN1 - n0) / Math.sqrt(n * 0.95 * 0.05 / 4.0);
        double pValue = Math.exp(-d * d / 2.0); // 단순화된 P-Value 계산

        return convertPValueToScore(pValue);
    }

    private double convertPValueToScore(double pValue) {
        // pValue가 0.01보다 크면 통계적으로 무작위하다고 간주
        if (pValue < 0.01) return pValue * 100; // 매우 낮은 점수
        return Math.min(100, 50 + (pValue * 50)); // 높은 점수
    }
}
