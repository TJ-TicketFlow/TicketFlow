from app.extensions import db


class Reservation(db.Model):
    __tablename__ = 'reservation' # MySQL에 만들어질 테이블 이름

    reservation_key = db.Column(db.BigInteger, primary_key=True)
    selected_seat_id = db.Column(db.BigInteger, db.ForeignKey('selected_seat.selected_seat_id'), nullable=False)
    reservation_date = db.Column(db.Date, nullable=False)
    reservation_count = db.Column(db.Integer, nullable=False)


    pay = db.relationship('Pay', backref='reservation', lazy=True)