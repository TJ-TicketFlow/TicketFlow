from app.extensions import db

class SelectedSeat(db.Model):
    __tablename__ = 'selected_seat'

    # 선택 좌석 PK
    selected_seat_id = db.Column(
        db.BigInteger,
        primary_key=True,
        autoincrement=True
    )

    # 회원 번호
    user_no = db.Column(
        db.BigInteger,
        db.ForeignKey('user.user_no'),
        nullable=False
    )

    # 좌석 ID
    seat_id = db.Column(
        db.String(20),
        db.ForeignKey('seat.seat_id'),
        nullable=False
    )

    # 좌석 상태
    # 0: 미선택
    # 1: 다른 유저 선택/결제중
    # 2: 선택완료
    seat_state = db.Column(
        db.SmallInteger,
        nullable=False,
        default=0
    )

    # 좌석 가격
    price = db.Column(
        db.BigInteger,
        nullable=False
    )

    reservation = db.relationship('Reservation', backref='selected_seat', lazy=True)