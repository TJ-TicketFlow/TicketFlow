from app.extensions import db

class MembershipHistory(db.Model):
    __tablename__ = 'membership_history'

    membership_history_id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    action_type = db.Column(db.String(50), nullable=False)
    previous_status = db.Column(db.String(20), nullable=False)
    new_status = db.Column(db.String(20), nullable=False)
    history_note = db.Column(db.Text, nullable=True)

    #외래키
    membership_id = db.Column(db.BigInteger, db.ForeignKey('membership.membership_id'), nullable=False)