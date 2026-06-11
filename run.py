from app import create_app

# Flask 애플리케이션 생성
app = create_app()

# 이 파일을 직접 실행했을 때만 서버를 켜도록 설정
if __name__ == '__main__':
    # debug=True는 코드를 수정하면 서버가 자동으로 새로고침 되게 해주는 편리한 옵션입니다.
    app.run(debug=True)