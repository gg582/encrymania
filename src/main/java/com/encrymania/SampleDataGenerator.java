package com.encrymania;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class SampleDataGenerator {
    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get("data"));

        byte[] key = new byte[16];
        new Random().nextBytes(key);
        try (FileOutputStream fos = new FileOutputStream("data/key.bin")) {
            fos.write(key);
        }

        // 블럭 분석을 위한 테스트 데이터입니다.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("This is a secret message that repeats... ");
        }
        byte[] plain = sb.toString().getBytes();
        try (FileOutputStream fos = new FileOutputStream("data/plain.txt")) {
            fos.write(plain);
        }

        // XOR 기반의 약한 난독화를 약한 암호화의 예시로 둡니다.
        byte[] weakEnc = new byte[plain.length];
        for (int i = 0; i < plain.length; i++) {
            weakEnc[i] = (byte) (plain[i] ^ key[i % key.length]);
        }
        try (FileOutputStream fos = new FileOutputStream("data/encrypted_weak.bin")) {
            fos.write(weakEnc);
        }

        System.out.println("Sample data generated in 'data' directory.");
        System.out.println("- data/key.bin (16 bytes random key)");
        System.out.println("- data/plain.txt (Repeated text)");
        System.out.println("- data/encrypted_weak.bin (Simple XOR encrypted)");
    }
}
