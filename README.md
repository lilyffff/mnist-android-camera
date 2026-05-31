# MNIST Camera Classifier (Android + TFLite)

TensorFlow Lite model (`mnist_nn_model.tflite`)을 사용해 안드로이드 폰 카메라로 손글씨 숫자를 촬영하고 0~9로 분류하는 앱입니다.

## 주요 기능

- CameraX 기반 카메라 미리보기 및 촬영
- 촬영 이미지 중앙 크롭 + 28x28 리사이즈
- grayscale + invert + MNIST 정규화
- TensorFlow Lite 추론 후 예측 숫자와 confidence 표시

## 프로젝트 구조

- `app/src/main/java/com/example/mnistcamera/MainActivity.kt`: 카메라 권한, CameraX 바인딩, 촬영/추론 흐름
- `app/src/main/java/com/example/mnistcamera/DigitClassifier.kt`: TFLite 로드 및 추론
- `app/src/main/java/com/example/mnistcamera/ImageUtils.kt`: 이미지 전처리
- `app/src/main/assets/mnist_nn_model.tflite`: 추론 모델

## 실행 방법

1. Android Studio에서 `mnist-android-camera` 폴더를 엽니다.
2. Gradle Sync를 완료합니다.
3. 실제 안드로이드 기기(카메라 권한 허용)로 실행합니다.
4. 종이에 손글씨 숫자를 쓰고 카메라로 촬영합니다.

## GitHub 배포

아래 명령으로 로컬에서 GitHub 저장소에 업로드할 수 있습니다.

1) 초기화

- `git init`
- `git add .`
- `git commit -m "Add Android CameraX MNIST classifier app"`

2) GitHub 저장소 연결

- `git branch -M main`
- `git remote add origin https://github.com/<your-id>/<repo-name>.git`
- `git push -u origin main`

## GitHub Actions workflow 배포

이 저장소에는 아래 2개의 자동화 workflow가 포함되어 있습니다.

- `.github/workflows/android-ci.yml`
	- `main` 브랜치 push 또는 PR 시 디버그 APK를 빌드
	- 결과물(`app-debug.apk`)을 Actions Artifact로 업로드
- `.github/workflows/android-release.yml`
	- `v*` 태그 push 시 릴리스 APK를 빌드
	- GitHub Release를 자동 생성하고 APK 첨부

릴리스 배포 예시:

- `git tag v1.0.0`
- `git push origin v1.0.0`

위 명령 실행 후 GitHub의 Actions/Release 탭에서 결과를 확인할 수 있습니다.

## 정확도 개선 팁

- 배경은 최대한 균일하고 밝게 유지
- 숫자는 프레임 중앙에 크게 배치
- 종이 배경만 보이도록 주변 배경 최소화
- 필요하면 전처리에 threshold/contour crop 추가
