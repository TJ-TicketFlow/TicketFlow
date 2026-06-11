import os
from flask import Flask
from app.extensions import db, ma
from app import models
from dotenv import load_dotenv

def create_app():
    # 1. Flask 웹 서버의 뼈대(app)를 만듭니다.
    app = Flask(__name__)

    load_dotenv() # .env 파일을 읽어서 os.environ 에 주입
    id = os.getenv('DB_USER')
    pwd = os.getenv('DB_PASSWORD')
    # DB 주소와 비밀번호 설정 (보통 config.py나 .env 파일에서 가져옵니다)
    app.config['SQLALCHEMY_DATABASE_URI'] = f"mysql+pymysql://{id}:{pwd}@localhost:3306/ticketflow"

    # 💡 여기서 Flask 앱(app)과 데이터베이스(db)가 드디어 합체합니다!
    db.init_app(app)

    # 💡 3. [여기 추가!] 재생 버튼 누를 때 테이블 자동 생성하는 마법의 코드
    with app.app_context():
        db.create_all() # 코드에 적힌 모든 모델을 읽어서 MySQL에 테이블을 자동 생성합니다.

    # 2. 화면이 잘 나오는지 확인하기 위한 임시 주소입니다.
    @app.route('/')
    def hello():
        return "환영합니다! 웹 서버가 정상적으로 켜졌습니다."

    # --- 여기서부터 추가할 부분 ---
    # app/routes/mypage.py 파일에서 방금 만든 mypage_bp 설계도를 가져옵니다.
    # from app.routes.mypage_routes import mypage_bp

    # 서버(app)에 설계도를 정식으로 등록합니다.
    # app.register_blueprint(mypage_bp)
    # --- 여기까지 ---

    return app

