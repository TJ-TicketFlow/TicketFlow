from app.extensions import db

class Seat(db.Model):
    __tablename__ = 'seat'

    seat_id = db.Column(
        db.String(20),
        primary_key=True
    )

    concert_id = db.Column(
        db.BigInteger,
        db.ForeignKey('concert.concert_id'),
        nullable=False
    )

    seat_class = db.Column(
        db.String(20),
        nullable=False
    )

    seat_status = db.Column(
        db.SmallInteger,
        nullable=False,
        default=1
    )

    seat_row = db.Column(
        db.String(50),
        nullable=False
    )

    seat_col = db.Column(
        db.String(50),
        nullable=False
    )

    selected_seat = db.relationship('SelectedSeat', backref='seat', lazy=True)
