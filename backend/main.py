import os
import sys
import threading
import time
from fastapi import FastAPI, HTTPException, Depends
from sqlalchemy import create_engine, Column, Integer, BigInteger, String, Double, ForeignKey, Text, UniqueConstraint, func
from sqlalchemy.orm import sessionmaker, Session, declarative_base
from pydantic import BaseModel, field_validator, ConfigDict
from typing import List, Optional, Any, Union
from datetime import date, datetime

def to_dict(model):
    if hasattr(model, "model_dump"):
        return model.model_dump()
    return model.dict()

# Configuración de la Base de Datos MySQL (XAMPP)
# Reemplaza 'root' y '' si tienes contraseña en MySQL
DATABASE_URL = "mysql+mysqlconnector://root:@localhost/aula_virtual"

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# --- Modelos SQLAlchemy ---

class UserDB(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    carnet = Column(String(10), unique=True, nullable=False)
    name = Column(String(255), nullable=False)
    password = Column(String(255), nullable=False)
    role = Column(String(50), nullable=False)
    address = Column(Text)
    personal_email = Column(String(255))
    profile_image = Column(Text)
    can_change_photo = Column(Integer, default=1)

class StudentDB(Base):
    __tablename__ = "students"
    id = Column(Integer, primary_key=True, index=True)
    encargadoId = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"))
    carnet = Column(String(20), unique=True, nullable=False)
    name = Column(String(255), nullable=False)
    grade = Column(String(100))
    profile_image = Column(Text)

class SubjectDB(Base):
    __tablename__ = "subjects"
    id = Column(Integer, primary_key=True, index=True)
    code = Column(String(50), unique=True, nullable=True)
    name = Column(String(255), nullable=False)
    description = Column(Text)
    professorId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    section = Column(String(10), default="01")
    color = Column(String(50), default="BLUE")

class EnrollmentDB(Base):
    __tablename__ = "enrollments"
    id = Column(Integer, primary_key=True, index=True)
    studentId = Column(Integer, ForeignKey("students.id", ondelete="CASCADE"))
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"))
    grade1 = Column(Double)
    grade2 = Column(Double)
    grade3 = Column(Double)
    grade4 = Column(Double)
    grade5 = Column(Double)
    average = Column(Double, nullable=True)
    color = Column(String(50), default="BLUE")
    __table_args__ = (UniqueConstraint('studentId', 'subjectId', name='_student_subject_uc'),)

class GradeImportLogDB(Base):
    __tablename__ = "grade_import_log"
    id = Column(Integer, primary_key=True, index=True)
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"), nullable=False)
    sheetName = Column(String(50), default="Notas")
    fileName = Column(String(255), nullable=False)
    fileHash = Column(String(64), nullable=False)
    importedAt = Column(String(100), nullable=False)
    rowsOk = Column(Integer, default=0)
    rowsError = Column(Integer, default=0)
    errorDetail = Column(Text, nullable=True)

class ConductLevelDB(Base):
    __tablename__ = "conduct_levels"
    value = Column(Integer, primary_key=True)
    label = Column(String(50), unique=True, nullable=False)

class ConductRecordDB(Base):
    __tablename__ = "conduct_records"
    id = Column(Integer, primary_key=True, index=True)
    studentId = Column(Integer, ForeignKey("students.id", ondelete="CASCADE"), nullable=False)
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"), nullable=False)
    period = Column(Integer, nullable=False)
    attention_participation = Column(Integer, nullable=True)
    rules_routines = Column(Integer, nullable=True)
    self_regulation_effort = Column(Integer, nullable=True)
    respect_collaboration = Column(Integer, nullable=True)
    responsibility = Column(Integer, nullable=True)
    observations = Column(Text, nullable=True)
    updatedAt = Column(String(100), nullable=True)
    average = Column(Double, nullable=True)
    __table_args__ = (UniqueConstraint('studentId', 'subjectId', 'period', name='_student_subject_period_uc'),)

class AcademicPeriodDB(Base):
    __tablename__ = "academic_periods"
    period = Column(Integer, primary_key=True)
    startDate = Column(String(10), nullable=False)
    endDate = Column(String(10), nullable=False)

class NonSchoolDayDB(Base):
    __tablename__ = "non_school_days"
    id = Column(Integer, primary_key=True, index=True)
    day = Column(String(10), unique=True, nullable=False)
    reason = Column(String(255), nullable=False)

class AttendanceStateDB(Base):
    __tablename__ = "attendance_states"
    value = Column(Integer, primary_key=True)
    label = Column(String(50), unique=True, nullable=False)

class AttendanceRecordDB(Base):
    __tablename__ = "attendance_records"
    id = Column(Integer, primary_key=True, index=True)
    studentId = Column(Integer, ForeignKey("students.id", ondelete="CASCADE"), nullable=False)
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"), nullable=False)
    classDate = Column(String(10), nullable=False)
    status = Column(Integer, default=0, nullable=False)
    justification = Column(Text, nullable=True)
    updatedAt = Column(String(100), nullable=True)
    __table_args__ = (UniqueConstraint('studentId', 'subjectId', 'classDate', name='_student_subject_classdate_uc'),)

class UserSubjectColorDB(Base):
    __tablename__ = "user_subject_colors"
    id = Column(Integer, primary_key=True, index=True)
    userId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"))
    color = Column(String(50), default="BLUE")
    __table_args__ = (UniqueConstraint('userId', 'subjectId', name='_user_subject_color_uc'),)

class BlogEntryDB(Base):
    __tablename__ = "blog_entries"
    id = Column(Integer, primary_key=True, index=True)
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"))
    category = Column(String(50)) # Parcial 1-5, Tarea, Info
    title = Column(String(255), nullable=False)
    content = Column(Text)
    position = Column(Integer, default=0)

class BlogCommentDB(Base):
    __tablename__ = "blog_comments"
    id = Column(Integer, primary_key=True, index=True)
    blogEntryId = Column(Integer, ForeignKey("blog_entries.id", ondelete="CASCADE"))
    userId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    content = Column(Text, nullable=False)
    timestamp = Column(String(50)) # Simplificado a string para el ejercicio

class FacilityDB(Base):
    __tablename__ = "facilities"
    id = Column(Integer, primary_key=True, index=True)
    code = Column(String(50), unique=True)
    name = Column(String(255), nullable=False)
    type = Column(String(100))
    description = Column(Text)

class FacilityScheduleDB(Base):
    __tablename__ = "facility_schedules"
    id = Column(Integer, primary_key=True, index=True)
    facilityId = Column(Integer, ForeignKey("facilities.id", ondelete="CASCADE"))
    subjectId = Column(Integer, ForeignKey("subjects.id", ondelete="CASCADE"))
    professorId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    days = Column(String(255))
    startTime = Column(String(10))
    endTime = Column(String(10))
    color = Column(String(50), default="BLUE")

class NotificationDB(Base):
    __tablename__ = "notifications"
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(255), nullable=False)
    message = Column(Text, nullable=False)
    targetType = Column(String(50), nullable=False)
    targetValue = Column(String(255), nullable=True)
    senderName = Column(String(255), nullable=False)
    timestamp = Column(String(100), nullable=False)

class DirectMessageDB(Base):
    __tablename__ = "direct_messages"
    id = Column(Integer, primary_key=True, index=True)
    chatRoomId = Column(String(100), nullable=False, index=True)
    senderId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    receiverId = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    message = Column(Text, nullable=False)
    timestamp = Column(String(100), nullable=False)
    isRead = Column(Integer, default=0)
    initiatorId = Column(Integer, nullable=False)

class ModificationLogDB(Base):
    __tablename__ = "modification_logs"
    entity_key = Column(String(100), primary_key=True)
    log_text = Column(String(255), nullable=False)
    updated_at = Column(String(100), nullable=False)
    updated_by = Column(String(150), nullable=False)

# Crear tablas si no existen
Base.metadata.create_all(bind=engine)

def seed_initial_data():
    try:
        db = SessionLocal()
        admin = db.query(UserDB).filter(UserDB.carnet == "ADMIN12345").first()
        if not admin:
            db.add(UserDB(carnet="ADMIN12345", name="Admin Maestro", password="ASD###", role="ADMIN"))
        else:
            if admin.password != "ASD###" or admin.role != "ADMIN":
                admin.password = "ASD###"
                admin.role = "ADMIN"
        db.commit()
        db.close()
    except Exception as e:
        print(f"Error seeding initial data in backend: {e}")

seed_initial_data()

# --- Esquemas Pydantic ---

class UserBase(BaseModel):
    carnet: str
    name: str
    password: str
    role: str
    address: Optional[str] = None
    personal_email: Optional[str] = None
    profile_image: Optional[str] = None
    can_change_photo: Optional[int] = 1

class User(UserBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class StudentBase(BaseModel):
    encargadoId: Optional[int] = None
    carnet: str
    name: str
    grade: Optional[str] = None
    profile_image: Optional[str] = None

class StudentSchema(StudentBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class SubjectBase(BaseModel):
    code: Optional[str] = None
    name: str
    description: Optional[str] = None
    professorId: Optional[int] = None
    section: Optional[str] = "01"
    color: Optional[str] = "BLUE"

class Subject(SubjectBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class EnrollmentBase(BaseModel):
    studentId: int
    subjectId: int
    grade1: Optional[float] = None
    grade2: Optional[float] = None
    grade3: Optional[float] = None
    grade4: Optional[float] = None
    grade5: Optional[float] = None
    average: Optional[float] = None
    color: Optional[str] = "BLUE"

class GradeImportLogBase(BaseModel):
    subjectId: int
    sheetName: Optional[str] = "Notas"
    fileName: str
    fileHash: str
    importedAt: str
    rowsOk: int = 0
    rowsError: int = 0
    errorDetail: Optional[str] = None

class GradeImportLogSchema(GradeImportLogBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class ConductLevelSchema(BaseModel):
    value: int
    label: str
    model_config = ConfigDict(from_attributes=True)

class ConductRecordBase(BaseModel):
    studentId: int
    subjectId: int
    period: int
    attention_participation: Optional[int] = None
    rules_routines: Optional[int] = None
    self_regulation_effort: Optional[int] = None
    respect_collaboration: Optional[int] = None
    responsibility: Optional[int] = None
    observations: Optional[str] = None
    updatedAt: Optional[str] = None
    average: Optional[float] = None

class ConductRecordSchema(ConductRecordBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class AcademicPeriodSchema(BaseModel):
    period: int
    startDate: Any
    endDate: Any

    @field_validator("startDate", "endDate", mode="before")
    @classmethod
    def format_period_dates(cls, v):
        if isinstance(v, (date, datetime)):
            return v.strftime("%Y-%m-%d")
        return str(v) if v is not None else ""

    model_config = ConfigDict(from_attributes=True)

class NonSchoolDayBase(BaseModel):
    day: Any
    reason: str

    @field_validator("day", mode="before")
    @classmethod
    def format_non_school_day(cls, v):
        if isinstance(v, (date, datetime)):
            return v.strftime("%Y-%m-%d")
        return str(v) if v is not None else ""

class NonSchoolDaySchema(NonSchoolDayBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class AttendanceRecordBase(BaseModel):
    studentId: int
    subjectId: int
    classDate: Any
    status: int = 0
    justification: Optional[str] = None
    updatedAt: Optional[str] = None

    @field_validator("classDate", mode="before")
    @classmethod
    def format_class_date(cls, v):
        if isinstance(v, (date, datetime)):
            return v.strftime("%Y-%m-%d")
        return str(v) if v is not None else ""

class AttendanceRecordSchema(AttendanceRecordBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class Enrollment(EnrollmentBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class UserSubjectColorBase(BaseModel):
    userId: int
    subjectId: int
    color: str = "BLUE"

class UserSubjectColor(UserSubjectColorBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class BlogEntryBase(BaseModel):
    subjectId: int
    category: str
    title: str
    content: Optional[str] = None
    position: Optional[int] = 0

class BlogEntry(BlogEntryBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class FacilityBase(BaseModel):
    code: Optional[str] = None
    name: str
    type: Optional[str] = None
    description: Optional[str] = None

class Facility(FacilityBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class FacilityScheduleBase(BaseModel):
    facilityId: int
    subjectId: int
    professorId: int
    days: str
    startTime: str
    endTime: str
    color: Optional[str] = "BLUE"

class FacilitySchedule(FacilityScheduleBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class ScheduleInfo(BaseModel):
    schedule: FacilitySchedule
    subjectName: str
    subjectSection: str = "01"
    subjectColor: str
    professorName: str

class BlogCommentBase(BaseModel):
    blogEntryId: int
    userId: int
    content: str
    timestamp: Optional[str] = None

class BlogComment(BlogCommentBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class CommentInfo(BaseModel):
    comment: BlogComment
    userName: str
    userRole: str
    userPhoto: Optional[str] = None

class NotificationBase(BaseModel):
    title: str
    message: str
    targetType: str
    targetValue: Optional[str] = None
    senderName: str
    timestamp: str

class NotificationSchema(NotificationBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class DirectMessageBase(BaseModel):
    chatRoomId: str
    senderId: int
    receiverId: int
    message: str
    timestamp: str
    isRead: int = 0
    initiatorId: int

class DirectMessageSchema(DirectMessageBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class ModificationLogBase(BaseModel):
    entity_key: str
    log_text: str
    updated_at: str
    updated_by: str

class ModificationLogSchema(ModificationLogBase):
    model_config = ConfigDict(from_attributes=True)

class ChatRoomInfoSchema(BaseModel):
    chatRoomId: str
    contactId: int
    contactName: str
    contactRole: str
    contactPhoto: Optional[str] = None
    contactSubjectOrGrade: Optional[str] = None
    lastMessage: str
    lastMessageTimestamp: str
    unreadCount: int
    initiatorId: int

# --- API FastAPI ---

app = FastAPI(title="Aula Virtual API")

# Dependencia para obtener la sesión
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# --- Endpoints ---

@app.get("/")
def root():
    return {
        "app": "Aula Virtual API",
        "status": "online",
        "docs": "/docs"
    }

@app.get("/users/students", response_model=List[User])
def get_students(db: Session = Depends(get_db)):
    return db.query(UserDB).filter(UserDB.role == "STUDENT").all()

@app.get("/users/admins", response_model=List[User])
def get_admins(db: Session = Depends(get_db)):
    return db.query(UserDB).filter(UserDB.role == "ADMIN").all()

@app.get("/users/professors", response_model=List[User])
def get_professors(db: Session = Depends(get_db)):
    return db.query(UserDB).filter(UserDB.role == "PROFESSOR").all()

@app.get("/users/encargados", response_model=List[User])
def get_encargados(db: Session = Depends(get_db)):
    return db.query(UserDB).filter(UserDB.role == "ENCARGADO").all()

# --- Student Records ---
@app.get("/students", response_model=List[StudentSchema])
def get_all_student_records(db: Session = Depends(get_db)):
    return db.query(StudentDB).all()

@app.get("/students/encargado/{encargado_id}", response_model=List[StudentSchema])
def get_students_by_encargado(encargado_id: int, db: Session = Depends(get_db)):
    return db.query(StudentDB).filter(StudentDB.encargadoId == encargado_id).all()

@app.post("/students", response_model=StudentSchema)
def create_student(student: StudentBase, db: Session = Depends(get_db)):
    db_student = StudentDB(**to_dict(student))
    db.add(db_student)
    db.commit()
    db.refresh(db_student)
    return db_student

@app.get("/students/{student_id}", response_model=StudentSchema)
def get_student_by_id(student_id: int, db: Session = Depends(get_db)):
    st = db.query(StudentDB).filter(StudentDB.id == student_id).first()
    if not st:
        raise HTTPException(status_code=404, detail="Estudiante no encontrado")
    return st

@app.put("/students/{student_id}", response_model=StudentSchema)
def update_student(student_id: int, student: StudentBase, db: Session = Depends(get_db)):
    db_st = db.query(StudentDB).filter(StudentDB.id == student_id).first()
    if not db_st:
        raise HTTPException(status_code=404, detail="Estudiante no encontrado")
    for key, value in to_dict(student).items():
        setattr(db_st, key, value)
    db.commit()
    db.refresh(db_st)
    return db_st

@app.delete("/students/{student_id}")
def delete_student(student_id: int, db: Session = Depends(get_db)):
    db_st = db.query(StudentDB).filter(StudentDB.id == student_id).first()
    if db_st:
        db.delete(db_st)
        db.commit()
    return {"message": "Estudiante eliminado"}

# --- User-specific Schedules ---
@app.get("/users/student/{student_id}/schedules", response_model=List[ScheduleInfo])
def get_student_timetable(student_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .join(EnrollmentDB, SubjectDB.id == EnrollmentDB.subjectId)\
        .filter(EnrollmentDB.studentId == student_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectSection": s.section if s.section else "01", "subjectColor": s.color, "professorName": u.name} for sch, s, u in results]

@app.get("/users/professor/{professor_id}/schedules", response_model=List[ScheduleInfo])
def get_professor_timetable(professor_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .filter(FacilityScheduleDB.professorId == professor_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectSection": s.section if s.section else "01", "subjectColor": s.color, "professorName": u.name} for sch, s, u in results]

@app.post("/users", response_model=User)
def create_user(user: UserBase, db: Session = Depends(get_db)):
    try:
        existing = db.query(UserDB).filter(UserDB.carnet == user.carnet).first()
        if existing:
            raise HTTPException(
                status_code=400,
                detail=f"El carnet '{user.carnet}' ya está registrado. Ingresa un número de carnet diferente."
            )
        db_user = UserDB(**to_dict(user))
        db.add(db_user)
        db.commit()
        db.refresh(db_user)
        return db_user
    except HTTPException:
        db.rollback()
        raise
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=f"Error al registrar usuario: {str(e)}")

@app.put("/users/{user_id}", response_model=User)
def update_user(user_id: int, user: UserBase, db: Session = Depends(get_db)):
    db_user = db.query(UserDB).filter(UserDB.id == user_id).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    for key, value in to_dict(user).items():
        setattr(db_user, key, value)
    db.commit()
    db.refresh(db_user)
    return db_user

@app.delete("/users/{user_id}")
def delete_user(user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(UserDB).filter(UserDB.id == user_id).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    db.delete(db_user)
    db.commit()
    return {"message": "Usuario eliminado"}

@app.get("/users/{user_id}", response_model=User)
def get_user_by_id(user_id: int, db: Session = Depends(get_db)):
    user = db.query(UserDB).filter(UserDB.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    return user

@app.get("/login")
def login(carnet: str, password: str, db: Session = Depends(get_db)):
    c = carnet.strip() if carnet else ""
    p = password.strip() if password else ""

    user = db.query(UserDB).filter(
        func.lower(UserDB.carnet) == c.lower(),
        UserDB.password == p
    ).first()
    if user:
        return user

    user = db.query(UserDB).filter(
        func.lower(UserDB.name) == c.lower(),
        UserDB.password == p
    ).first()
    if user:
        return user

    if c.upper() in ["ADMIN", "ADMIN12345", "ADMIN MAESTRO"] and p == "ASD###":
        user = db.query(UserDB).filter(UserDB.carnet == "ADMIN12345").first()
        if not user:
            user = UserDB(carnet="ADMIN12345", name="Admin Maestro", password="ASD###", role="ADMIN")
            db.add(user)
            db.commit()
            db.refresh(user)
        else:
            if user.role != "ADMIN" or user.password != "ASD###":
                user.role = "ADMIN"
                user.password = "ASD###"
                db.commit()
                db.refresh(user)
        return user

    raise HTTPException(status_code=401, detail="Credenciales incorrectas")

# --- Subjects ---
@app.get("/subjects", response_model=List[Subject])
def get_subjects(db: Session = Depends(get_db)):
    return db.query(SubjectDB).all()

@app.post("/subjects", response_model=Subject)
def create_subject(subject: SubjectBase, db: Session = Depends(get_db)):
    data = to_dict(subject)
    if not data.get("code"):
        import random
        data["code"] = f"ASIGN{random.randint(10000, 99999)}"
    db_sub = SubjectDB(**data)
    db.add(db_sub)
    db.commit()
    db.refresh(db_sub)
    return db_sub

@app.put("/subjects/{sub_id}", response_model=Subject)
def update_subject(sub_id: int, subject: SubjectBase, db: Session = Depends(get_db)):
    db_sub = db.query(SubjectDB).filter(SubjectDB.id == sub_id).first()
    if not db_sub:
        raise HTTPException(status_code=404, detail="Materia no encontrada")
    for key, value in to_dict(subject).items():
        setattr(db_sub, key, value)
    db.commit()
    db.refresh(db_sub)
    return db_sub

@app.delete("/subjects/{sub_id}")
def delete_subject(sub_id: int, db: Session = Depends(get_db)):
    db_sub = db.query(SubjectDB).filter(SubjectDB.id == sub_id).first()
    if not db_sub:
        raise HTTPException(status_code=404, detail="Materia no encontrada")
    db.delete(db_sub)
    db.commit()
    return {"message": "Materia eliminada"}

@app.get("/subjects/{sub_id}", response_model=Subject)
def get_subject_by_id(sub_id: int, db: Session = Depends(get_db)):
    sub = db.query(SubjectDB).filter(SubjectDB.id == sub_id).first()
    if not sub:
        raise HTTPException(status_code=404, detail="Materia no encontrada")
    return sub

@app.get("/subjects/professor/{prof_id}", response_model=List[Subject])
def get_subjects_by_professor(prof_id: int, db: Session = Depends(get_db)):
    return db.query(SubjectDB).filter(
        (SubjectDB.professorId == prof_id) |
        (SubjectDB.id.in_(db.query(FacilityScheduleDB.subjectId).filter(FacilityScheduleDB.professorId == prof_id)))
    ).distinct().all()

# --- Enrollments ---
class StudentGradeInfo(BaseModel):
    enrollment: Enrollment
    subject: Subject

class StudentInSubjectInfo(BaseModel):
    enrollment: Enrollment
    student: User

@app.get("/subjects/{sub_id}/enrollments", response_model=List[StudentInSubjectInfo])
def get_subject_enrollments(sub_id: int, db: Session = Depends(get_db)):
    enrollments = db.query(EnrollmentDB).filter(EnrollmentDB.subjectId == sub_id).all()
    output = []
    for en in enrollments:
        student = db.query(StudentDB).filter(StudentDB.id == en.studentId).first()
        if student:
            stu_user = UserDB(
                id=student.id,
                carnet=student.carnet,
                name=student.name,
                password="ASD###",
                role="STUDENT",
                profile_image=student.profile_image
            )
            output.append({"enrollment": en, "student": stu_user})
    return output

@app.get("/enrollments/student/{student_id}", response_model=List[StudentGradeInfo])
def get_student_grades(student_id: int, db: Session = Depends(get_db)):
    results = db.query(EnrollmentDB, SubjectDB).join(SubjectDB, EnrollmentDB.subjectId == SubjectDB.id).filter(EnrollmentDB.studentId == student_id).all()
    return [{"enrollment": e, "subject": s} for e, s in results]

@app.post("/enrollments", response_model=Enrollment)
def enroll_student(enrollment: EnrollmentBase, db: Session = Depends(get_db)):
    # Verificar si ya existe la inscripción
    existing = db.query(EnrollmentDB).filter(
        EnrollmentDB.studentId == enrollment.studentId,
        EnrollmentDB.subjectId == enrollment.subjectId
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="El alumno ya está inscrito en esta materia")

    db_en = EnrollmentDB(**to_dict(enrollment))
    db.add(db_en)
    db.commit()
    db.refresh(db_en)
    return db_en

@app.put("/enrollments/{en_id}", response_model=Enrollment)
def update_enrollment(en_id: int, enrollment: EnrollmentBase, db: Session = Depends(get_db)):
    db_en = db.query(EnrollmentDB).filter(EnrollmentDB.id == en_id).first()
    if not db_en:
        raise HTTPException(status_code=404, detail="Inscripción no encontrada")
    for key, value in to_dict(enrollment).items():
        setattr(db_en, key, value)
    db.commit()
    db.refresh(db_en)
    return db_en

@app.delete("/enrollments/{en_id}")
def delete_enrollment(en_id: int, db: Session = Depends(get_db)):
    db_en = db.query(EnrollmentDB).filter(EnrollmentDB.id == en_id).first()
    if not db_en:
        raise HTTPException(status_code=404, detail="Inscripción no encontrada")
    db.delete(db_en)
    db.commit()
    return {"message": "Inscripción eliminada"}

@app.get("/enrollments/{en_id}", response_model=Enrollment)
def get_enrollment_by_id(en_id: int, db: Session = Depends(get_db)):
    en = db.query(EnrollmentDB).filter(EnrollmentDB.id == en_id).first()
    if not en:
        raise HTTPException(status_code=404, detail="Inscripción no encontrada")
    return en

# --- Blog Entries ---
@app.get("/subjects/{sub_id}/blog", response_model=List[BlogEntry])
def get_subject_blog(sub_id: int, db: Session = Depends(get_db)):
    try:
        return db.query(BlogEntryDB).filter(BlogEntryDB.subjectId == sub_id).order_by(BlogEntryDB.position.asc()).all()
    except Exception as e:
        print(f"Error al obtener blog: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")

@app.post("/blog", response_model=BlogEntry)
def create_blog_entry(entry: BlogEntryBase, db: Session = Depends(get_db)):
    try:
        # Find minimum position to put new entry at top
        min_pos = db.query(BlogEntryDB).filter(BlogEntryDB.subjectId == entry.subjectId).order_by(BlogEntryDB.position.asc()).first()
        new_pos = (min_pos.position - 1) if min_pos else 0

        db_entry = BlogEntryDB(**to_dict(entry))
        db_entry.position = new_pos
        db.add(db_entry)
        db.commit()
        db.refresh(db_entry)
        return db_entry
    except Exception as e:
        db.rollback()
        print(f"Error al crear blog: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")

@app.put("/blog/reorder")
def reorder_blog(entries: List[BlogEntry], db: Session = Depends(get_db)):
    try:
        for idx, entry in enumerate(entries):
            db.query(BlogEntryDB).filter(BlogEntryDB.id == entry.id).update({BlogEntryDB.position: idx})
        db.commit()
        return {"message": "Orden actualizado"}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@app.put("/blog/{entry_id}", response_model=BlogEntry)
def update_blog_entry(entry_id: int, entry: BlogEntryBase, db: Session = Depends(get_db)):
    db_entry = db.query(BlogEntryDB).filter(BlogEntryDB.id == entry_id).first()
    if not db_entry:
        raise HTTPException(status_code=404, detail="Entrada no encontrada")
    for key, value in to_dict(entry).items():
        setattr(db_entry, key, value)
    db.commit()
    db.refresh(db_entry)
    return db_entry

@app.delete("/blog/{entry_id}")
def delete_blog_entry(entry_id: int, db: Session = Depends(get_db)):
    db_entry = db.query(BlogEntryDB).filter(BlogEntryDB.id == entry_id).first()
    if not db_entry:
        raise HTTPException(status_code=404, detail="Entrada no encontrada")
    db.delete(db_entry)
    db.commit()
    return {"message": "Entrada eliminada"}

# --- Blog Comments ---
@app.get("/blog/{entry_id}/comments", response_model=List[CommentInfo])
def get_blog_comments(entry_id: int, db: Session = Depends(get_db)):
    results = db.query(BlogCommentDB, UserDB).join(UserDB, BlogCommentDB.userId == UserDB.id).filter(BlogCommentDB.blogEntryId == entry_id).order_by(BlogCommentDB.id.asc()).all()
    return [{"comment": c, "userName": u.name, "userRole": u.role, "userPhoto": u.profile_image} for c, u in results]

@app.post("/blog/comments", response_model=BlogComment)
def create_blog_comment(comment: BlogCommentBase, db: Session = Depends(get_db)):
    db_comment = BlogCommentDB(**to_dict(comment))
    if not db_comment.timestamp:
        from datetime import datetime
        db_comment.timestamp = datetime.now().strftime("%d/%m/%Y %I:%M %p")
    db.add(db_comment)
    db.commit()
    db.refresh(db_comment)
    return db_comment

@app.delete("/blog/comments/{comment_id}")
def delete_blog_comment(comment_id: int, db: Session = Depends(get_db)):
    db_comment = db.query(BlogCommentDB).filter(BlogCommentDB.id == comment_id).first()
    if not db_comment:
        raise HTTPException(status_code=404, detail="Comentario no encontrado")
    db.delete(db_comment)
    db.commit()
    return {"message": "Comentario eliminado"}

# --- Facilities ---
@app.get("/facilities", response_model=List[Facility])
def get_facilities(db: Session = Depends(get_db)):
    return db.query(FacilityDB).all()

@app.post("/facilities", response_model=Facility)
def create_facility(facility: FacilityBase, db: Session = Depends(get_db)):
    db_fac = FacilityDB(**to_dict(facility))
    db.add(db_fac)
    db.commit()
    db.refresh(db_fac)
    if not db_fac.code:
        db_fac.code = f"INSTA{10000 + db_fac.id}"
        db.commit()
        db.refresh(db_fac)
    return db_fac

@app.put("/facilities/{fac_id}", response_model=Facility)
def update_facility(fac_id: int, facility: FacilityBase, db: Session = Depends(get_db)):
    db_fac = db.query(FacilityDB).filter(FacilityDB.id == fac_id).first()
    if not db_fac:
        raise HTTPException(status_code=404, detail="Instalación no encontrada")
    for key, value in to_dict(facility).items():
        setattr(db_fac, key, value)
    db.commit()
    db.refresh(db_fac)
    return db_fac

@app.delete("/facilities/{fac_id}")
def delete_facility(fac_id: int, db: Session = Depends(get_db)):
    db_fac = db.query(FacilityDB).filter(FacilityDB.id == fac_id).first()
    if not db_fac:
        raise HTTPException(status_code=404, detail="Instalación no encontrada")
    db.delete(db_fac)
    db.commit()
    return {"message": "Instalación eliminada"}

# --- Facility Schedules ---
@app.get("/facilities/{fac_id}/schedules", response_model=List[ScheduleInfo])
def get_facility_schedules(fac_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .filter(FacilityScheduleDB.facilityId == fac_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectSection": s.section if s.section else "01", "subjectColor": sch.color if sch.color else (s.color if s.color else "BLUE"), "professorName": u.name} for sch, s, u in results]

def time_to_minutes(time_str: str) -> int:
    try:
        parts = time_str.strip().split()
        time_parts = parts[0].split(':')
        hour = int(time_parts[0])
        minute = int(time_parts[1])
        if len(parts) > 1:
            ampm = parts[1].upper()
            if ampm == "PM" and hour < 12:
                hour += 12
            elif ampm == "AM" and hour == 12:
                hour = 0
        return hour * 60 + minute
    except Exception:
        return 0

def check_schedule_conflicts_backend(schedule: FacilityScheduleBase, db: Session, current_id: Optional[int] = None):
    new_start = time_to_minutes(schedule.startTime)
    new_end = time_to_minutes(schedule.endTime)
    if new_start >= new_end:
        raise HTTPException(status_code=400, detail="La hora de inicio debe ser menor a la hora de finalización.")

    # Normalizar días removiendo espacios extras
    new_days = [d.strip() for d in schedule.days.replace(', ', ',').split(',')]

    query = db.query(FacilityScheduleDB).filter(
        (FacilityScheduleDB.facilityId == schedule.facilityId) |
        (FacilityScheduleDB.professorId == schedule.professorId)
    )
    if current_id:
        query = query.filter(FacilityScheduleDB.id != current_id)

    for ex in query.all():
        ex_days = [d.strip() for d in ex.days.replace(', ', ',').split(',')]
        if any(d in ex_days for d in new_days):
            ex_start = time_to_minutes(ex.startTime)
            ex_end = time_to_minutes(ex.endTime)
            if new_start < ex_end and new_end > ex_start:
                if ex.facilityId == schedule.facilityId:
                    raise HTTPException(status_code=400, detail="Esta aula ya está ocupada en los días y rango de horario solicitados.")
                else:
                    raise HTTPException(status_code=400, detail="El profesor ya tiene un horario asignado en este mismo intervalo.")

@app.post("/facilities/schedules", response_model=FacilitySchedule)
def create_schedule(schedule: FacilityScheduleBase, db: Session = Depends(get_db)):
    check_schedule_conflicts_backend(schedule, db)
    db_sch = FacilityScheduleDB(**to_dict(schedule))
    db.add(db_sch)
    db.commit()
    db.refresh(db_sch)
    return db_sch

@app.put("/facilities/schedules/{sch_id}", response_model=FacilitySchedule)
def update_schedule(sch_id: int, schedule: FacilityScheduleBase, db: Session = Depends(get_db)):
    check_schedule_conflicts_backend(schedule, db, current_id=sch_id)
    db_sch = db.query(FacilityScheduleDB).filter(FacilityScheduleDB.id == sch_id).first()
    if not db_sch:
        raise HTTPException(status_code=404, detail="Horario no encontrado")
    for key, value in to_dict(schedule).items():
        setattr(db_sch, key, value)
    db.commit()
    db.refresh(db_sch)
    return db_sch

@app.delete("/facilities/schedules/{sch_id}")
def delete_schedule(sch_id: int, db: Session = Depends(get_db)):
    db_sch = db.query(FacilityScheduleDB).filter(FacilityScheduleDB.id == sch_id).first()
    if not db_sch:
        raise HTTPException(status_code=404, detail="Horario no encontrado")
    db.delete(db_sch)
    db.commit()
    return {"message": "Horario eliminado"}

@app.get("/facilities/schedules/{sch_id}")
def get_schedule_by_id(sch_id: int, db: Session = Depends(get_db)):
    sch = db.query(FacilityScheduleDB).filter(FacilityScheduleDB.id == sch_id).first()
    if not sch:
        raise HTTPException(status_code=404, detail="Horario no encontrado")
    return sch

@app.get("/facilities/schedules/subject/{sub_id}", response_model=List[FacilitySchedule])
def get_facility_schedules_by_subject(sub_id: int, db: Session = Depends(get_db)):
    return db.query(FacilityScheduleDB).filter(FacilityScheduleDB.subjectId == sub_id).all()

# --- User specific schedules & subject colors ---
@app.put("/users/{user_id}/subjects/{subject_id}/color")
def update_user_subject_color(user_id: int, subject_id: int, color: str, db: Session = Depends(get_db)):
    usc = db.query(UserSubjectColorDB).filter(UserSubjectColorDB.userId == user_id, UserSubjectColorDB.subjectId == subject_id).first()
    if not usc:
        usc = UserSubjectColorDB(userId=user_id, subjectId=subject_id, color=color)
        db.add(usc)
    else:
        usc.color = color

    enrollment = db.query(EnrollmentDB).filter(EnrollmentDB.studentId == user_id, EnrollmentDB.subjectId == subject_id).first()
    if enrollment:
        enrollment.color = color

    db.commit()
    return {"message": "Color actualizado", "color": color}

@app.get("/users/{user_id}/subjects/{subject_id}/color")
def get_user_subject_color(user_id: int, subject_id: int, db: Session = Depends(get_db)):
    usc = db.query(UserSubjectColorDB).filter(UserSubjectColorDB.userId == user_id, UserSubjectColorDB.subjectId == subject_id).first()
    if usc and usc.color:
        return {"color": usc.color}
    enrollment = db.query(EnrollmentDB).filter(EnrollmentDB.studentId == user_id, EnrollmentDB.subjectId == subject_id).first()
    if enrollment and enrollment.color:
        return {"color": enrollment.color}
    subject = db.query(SubjectDB).filter(SubjectDB.id == subject_id).first()
    return {"color": subject.color if subject and subject.color else "BLUE"}

@app.get("/users/student/{student_id}/schedules", response_model=List[ScheduleInfo])
def get_student_personal_schedules(student_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB, EnrollmentDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .join(EnrollmentDB, SubjectDB.id == EnrollmentDB.subjectId)\
        .filter(EnrollmentDB.studentId == student_id).all()

    return [{"schedule": sch, "subjectName": s.name, "subjectSection": s.section if s.section else "01", "subjectColor": e.color if e.color else (s.color if s.color else "BLUE"), "professorName": u.name} for sch, s, u, e in results]

@app.get("/users/professor/{prof_id}/schedules", response_model=List[ScheduleInfo])
def get_professor_personal_schedules(prof_id: int, db: Session = Depends(get_db)):
    results = db.query(FacilityScheduleDB, SubjectDB, UserDB)\
        .join(SubjectDB, FacilityScheduleDB.subjectId == SubjectDB.id)\
        .join(UserDB, FacilityScheduleDB.professorId == UserDB.id)\
        .filter(FacilityScheduleDB.professorId == prof_id).all()

    output = []
    for sch, s, u in results:
        usc = db.query(UserSubjectColorDB).filter(UserSubjectColorDB.userId == prof_id, UserSubjectColorDB.subjectId == s.id).first()
        color = usc.color if (usc and usc.color) else (s.color if s.color else "BLUE")
        output.append({"schedule": sch, "subjectName": s.name, "subjectSection": s.section if s.section else "01", "subjectColor": color, "professorName": u.name})
    return output

@app.delete("/blog/entries/{entry_id}/comments")
def clear_blog_discussion(entry_id: int, db: Session = Depends(get_db)):
    db.query(BlogCommentDB).filter(BlogCommentDB.blogEntryId == entry_id).delete()
    db.commit()
    return {"message": "Conversación vaciada"}

# --- Notifications ---
@app.get("/notifications", response_model=List[NotificationSchema])
def get_notifications(db: Session = Depends(get_db)):
    return db.query(NotificationDB).order_by(NotificationDB.id.desc()).all()

@app.post("/notifications", response_model=NotificationSchema)
def create_notification(notification: NotificationBase, db: Session = Depends(get_db)):
    from datetime import datetime

    # 1. Enforce Server Time 2-Hour Limit for SUPPORT Messages
    if notification.targetType == "SUPPORT":
        last_support = db.query(NotificationDB).filter(
            NotificationDB.targetType == "SUPPORT",
            NotificationDB.senderName == notification.senderName
        ).order_by(NotificationDB.id.desc()).first()

        if last_support and last_support.timestamp:
            try:
                last_dt = datetime.strptime(last_support.timestamp, "%d/%m/%Y %H:%M")
                now_dt = datetime.now()
                elapsed_seconds = (now_dt - last_dt).total_seconds()
                if elapsed_seconds < 7200:
                    remaining_mins = int((7200 - elapsed_seconds) / 60)
                    hours = remaining_mins // 60
                    mins = remaining_mins % 60
                    time_msg = f"{hours} hora(s) y {mins} minuto(s)" if hours > 0 else f"{mins} minuto(s)"
                    raise HTTPException(status_code=400, detail=f"Debes esperar {time_msg} antes de enviar otro reporte a los Administradores.")
            except ValueError:
                pass

    # 2. Always stamp exact server time
    notif_data = to_dict(notification)
    notif_data["timestamp"] = datetime.now().strftime("%d/%m/%Y %H:%M")

    db_notif = NotificationDB(**notif_data)
    db.add(db_notif)
    db.commit()
    db.refresh(db_notif)
    return db_notif

@app.put("/notifications/{id}", response_model=NotificationSchema)
def update_notification(id: int, notification: NotificationBase, db: Session = Depends(get_db)):
    db_notif = db.query(NotificationDB).filter(NotificationDB.id == id).first()
    if not db_notif:
        raise HTTPException(status_code=404, detail="Notificación no encontrada")
    for key, value in to_dict(notification).items():
        setattr(db_notif, key, value)
    db.commit()
    db.refresh(db_notif)
    return db_notif

@app.delete("/notifications/{id}")
def delete_notification(id: int, db: Session = Depends(get_db)):
    db_notif = db.query(NotificationDB).filter(NotificationDB.id == id).first()
    if not db_notif:
        raise HTTPException(status_code=404, detail="Notificación no encontrada")
    db.delete(db_notif)
    db.commit()
    return {"message": "Notificación eliminada"}

# --- Direct Messages Endpoints ---

@app.get("/direct-messages/rooms/{user_id}", response_model=List[ChatRoomInfoSchema])
def get_user_chat_rooms(user_id: int, db: Session = Depends(get_db)):
    messages = db.query(DirectMessageDB).filter(
        (DirectMessageDB.senderId == user_id) | (DirectMessageDB.receiverId == user_id)
    ).order_by(DirectMessageDB.id.desc()).all()

    rooms_map = {}
    for msg in messages:
        if msg.chatRoomId in rooms_map:
            continue

        contact_id = msg.receiverId if msg.senderId == user_id else msg.senderId
        contact = db.query(UserDB).filter(UserDB.id == contact_id).first()
        if not contact:
            continue

        unread = db.query(DirectMessageDB).filter(
            DirectMessageDB.chatRoomId == msg.chatRoomId,
            DirectMessageDB.receiverId == user_id,
            DirectMessageDB.isRead == 0
        ).count()

        subj_or_grade = None
        if contact.role == "PROFESSOR":
            subj = db.query(SubjectDB).filter(SubjectDB.professorId == contact.id).first()
            if subj:
                subj_or_grade = f"Profesor • {subj.name}"
            else:
                subj_or_grade = "Profesor"
        elif contact.role == "ADMIN":
            subj_or_grade = "Administración Central"
        elif contact.role == "ENCARGADO":
            subj_or_grade = "Encargado de Familia"
        else:
            st = db.query(StudentDB).filter(StudentDB.id == contact.id).first()
            if st and st.grade:
                subj_or_grade = f"Estudiante • {st.grade}"
            else:
                subj_or_grade = "Estudiante"

        rooms_map[msg.chatRoomId] = ChatRoomInfoSchema(
            chatRoomId=msg.chatRoomId,
            contactId=contact.id,
            contactName=contact.name,
            contactRole=contact.role,
            contactPhoto=contact.profile_image,
            contactSubjectOrGrade=subj_or_grade,
            lastMessage=msg.message,
            lastMessageTimestamp=msg.timestamp,
            unreadCount=unread,
            initiatorId=msg.initiatorId
        )

    return list(rooms_map.values())

@app.get("/direct-messages/history/{user_id}/{contact_id}", response_model=List[DirectMessageSchema])
def get_direct_messages_history(user_id: int, contact_id: int, db: Session = Depends(get_db)):
    msgs = db.query(DirectMessageDB).filter(
        ((DirectMessageDB.senderId == user_id) & (DirectMessageDB.receiverId == contact_id)) |
        ((DirectMessageDB.senderId == contact_id) & (DirectMessageDB.receiverId == user_id))
    ).order_by(DirectMessageDB.id.asc()).all()

    db.query(DirectMessageDB).filter(
        DirectMessageDB.receiverId == user_id,
        DirectMessageDB.senderId == contact_id,
        DirectMessageDB.isRead == 0
    ).update({"isRead": 1})
    db.commit()

    return msgs

@app.post("/direct-messages", response_model=DirectMessageSchema)
def send_direct_message(msg: DirectMessageBase, db: Session = Depends(get_db)):
    sender = db.query(UserDB).filter(UserDB.id == msg.senderId).first()
    receiver = db.query(UserDB).filter(UserDB.id == msg.receiverId).first()
    if not sender or not receiver:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    if receiver.role == "ADMIN" and sender.role != "ADMIN":
        prior_admin_msg = db.query(DirectMessageDB).filter(
            DirectMessageDB.chatRoomId == msg.chatRoomId,
            DirectMessageDB.senderId == receiver.id
        ).first()
        if not prior_admin_msg and msg.initiatorId != receiver.id:
            raise HTTPException(
                status_code=403,
                detail="Solo un Administrador puede iniciar la conversación directa."
            )

    db_msg = DirectMessageDB(**to_dict(msg))
    db.add(db_msg)
    db.commit()
    db.refresh(db_msg)
    return db_msg

@app.delete("/direct-messages/rooms/{chat_room_id}")
def delete_chat_room(chat_room_id: str, db: Session = Depends(get_db)):
    db.query(DirectMessageDB).filter(DirectMessageDB.chatRoomId == chat_room_id).delete()
    db.commit()
    return {"message": "Chat eliminado exitosamente"}

@app.get("/subjects/{sub_id}/participants", response_model=List[User])
def get_subject_participants(sub_id: int, db: Session = Depends(get_db)):
    subject = db.query(SubjectDB).filter(SubjectDB.id == sub_id).first()
    if not subject:
        raise HTTPException(status_code=404, detail="Materia no encontrada")

    participants = []
    added_user_ids = set()
    added_student_ids = set()

    # 1. Add Professor assigned via subject.professorId
    if subject.professorId:
        prof = db.query(UserDB).filter(UserDB.id == subject.professorId, UserDB.role != "ADMIN").first()
        if prof and prof.id not in added_user_ids:
            participants.append(prof)
            added_user_ids.add(prof.id)

    # 2. Add Professors assigned via facility_schedules
    professors = db.query(UserDB).join(FacilityScheduleDB, UserDB.id == FacilityScheduleDB.professorId).filter(
        FacilityScheduleDB.subjectId == sub_id,
        UserDB.role != "ADMIN"
    ).distinct().all()
    for p in professors:
        if p.id not in added_user_ids:
            participants.append(p)
            added_user_ids.add(p.id)

    # 3. Add Enrolled Students
    enrollments = db.query(EnrollmentDB).filter(EnrollmentDB.subjectId == sub_id).all()
    for en in enrollments:
        student = db.query(StudentDB).filter(StudentDB.id == en.studentId).first()
        if student and student.id not in added_student_ids:
            stu_user = UserDB(
                id=student.id,
                carnet=student.carnet,
                name=student.name,
                password="ASD###",
                role="STUDENT",
                profile_image=student.profile_image
            )
            participants.append(stu_user)
            added_student_ids.add(student.id)

    return participants

# --- Grade Import Log Endpoints ---

@app.get("/subjects/{sub_id}/grade-import-logs", response_model=List[GradeImportLogSchema])
def get_subject_grade_import_logs(sub_id: int, db: Session = Depends(get_db)):
    return db.query(GradeImportLogDB).filter(GradeImportLogDB.subjectId == sub_id).order_by(GradeImportLogDB.id.desc()).all()

@app.post("/grade-import-logs", response_model=GradeImportLogSchema)
def create_grade_import_log(log: GradeImportLogBase, db: Session = Depends(get_db)):
    db_log = GradeImportLogDB(**to_dict(log))
    db.add(db_log)
    db.commit()
    db.refresh(db_log)
    return db_log

# --- Conduct Endpoints ---

@app.get("/conduct-levels", response_model=List[ConductLevelSchema])
def get_conduct_levels(db: Session = Depends(get_db)):
    return db.query(ConductLevelDB).order_by(ConductLevelDB.value.asc()).all()

@app.get("/subjects/{sub_id}/conduct-records", response_model=List[ConductRecordSchema])
def get_subject_conduct_records(sub_id: int, db: Session = Depends(get_db)):
    return db.query(ConductRecordDB).filter(ConductRecordDB.subjectId == sub_id).order_by(ConductRecordDB.studentId.asc(), ConductRecordDB.period.asc()).all()

@app.get("/students/{student_id}/subjects/{sub_id}/conduct-records", response_model=List[ConductRecordSchema])
def get_student_conduct_records(student_id: int, sub_id: int, db: Session = Depends(get_db)):
    return db.query(ConductRecordDB).filter(
        ConductRecordDB.studentId == student_id,
        ConductRecordDB.subjectId == sub_id
    ).order_by(ConductRecordDB.period.asc()).all()

@app.post("/conduct-records", response_model=ConductRecordSchema)
def upsert_conduct_record(rec: ConductRecordBase, db: Session = Depends(get_db)):
    existing = db.query(ConductRecordDB).filter(
        ConductRecordDB.studentId == rec.studentId,
        ConductRecordDB.subjectId == rec.subjectId,
        ConductRecordDB.period == rec.period
    ).first()

    data = to_dict(rec)
    if existing:
        for k, v in data.items():
            setattr(existing, k, v)
        db.commit()
        db.refresh(existing)
        return existing
    else:
        new_rec = ConductRecordDB(**data)
        db.add(new_rec)
        db.commit()
        db.refresh(new_rec)
        return new_rec

# --- Academic Calendar Endpoints ---

@app.get("/academic-periods", response_model=List[AcademicPeriodSchema])
def get_academic_periods(db: Session = Depends(get_db)):
    periods = db.query(AcademicPeriodDB).order_by(AcademicPeriodDB.period.asc()).all()
    for p in periods:
        if isinstance(p.startDate, (date, datetime)):
            p.startDate = p.startDate.strftime("%Y-%m-%d")
        else:
            p.startDate = str(p.startDate) if p.startDate is not None else ""
        if isinstance(p.endDate, (date, datetime)):
            p.endDate = p.endDate.strftime("%Y-%m-%d")
        else:
            p.endDate = str(p.endDate) if p.endDate is not None else ""
    return periods

@app.post("/academic-periods", response_model=List[AcademicPeriodSchema])
def save_academic_periods(periods: List[AcademicPeriodSchema], db: Session = Depends(get_db)):
    db.query(AcademicPeriodDB).delete()
    for p in periods:
        p_dict = to_dict(p)
        if isinstance(p_dict.get("startDate"), (date, datetime)):
            p_dict["startDate"] = p_dict["startDate"].strftime("%Y-%m-%d")
        else:
            p_dict["startDate"] = str(p_dict.get("startDate", ""))
        if isinstance(p_dict.get("endDate"), (date, datetime)):
            p_dict["endDate"] = p_dict["endDate"].strftime("%Y-%m-%d")
        else:
            p_dict["endDate"] = str(p_dict.get("endDate", ""))
        db.add(AcademicPeriodDB(**p_dict))
    db.commit()
    return get_academic_periods(db)

@app.get("/non-school-days", response_model=List[NonSchoolDaySchema])
def get_non_school_days(db: Session = Depends(get_db)):
    days = db.query(NonSchoolDayDB).order_by(NonSchoolDayDB.day.asc()).all()
    for d in days:
        if isinstance(d.day, (date, datetime)):
            d.day = d.day.strftime("%Y-%m-%d")
        else:
            d.day = str(d.day) if d.day is not None else ""
    return days

@app.post("/non-school-days", response_model=NonSchoolDaySchema)
def add_non_school_day(day: NonSchoolDayBase, db: Session = Depends(get_db)):
    d_dict = to_dict(day)
    if isinstance(d_dict.get("day"), (date, datetime)):
        d_dict["day"] = d_dict["day"].strftime("%Y-%m-%d")
    else:
        d_dict["day"] = str(d_dict.get("day", ""))
    db_day = NonSchoolDayDB(**d_dict)
    db.add(db_day)
    db.commit()
    db.refresh(db_day)
    if isinstance(db_day.day, (date, datetime)):
        db_day.day = db_day.day.strftime("%Y-%m-%d")
    else:
        db_day.day = str(db_day.day) if db_day.day is not None else ""
    return db_day

@app.delete("/non-school-days/{day_id}")
def delete_non_school_day(day_id: int, db: Session = Depends(get_db)):
    db.query(NonSchoolDayDB).filter(NonSchoolDayDB.id == day_id).delete()
    db.commit()
    return {"message": "Día no lectivo eliminado"}

# --- Attendance Endpoints ---

@app.get("/subjects/{sub_id}/attendance-records", response_model=List[AttendanceRecordSchema])
def get_subject_attendance_records(sub_id: int, db: Session = Depends(get_db)):
    return db.query(AttendanceRecordDB).filter(AttendanceRecordDB.subjectId == sub_id).order_by(AttendanceRecordDB.classDate.asc(), AttendanceRecordDB.studentId.asc()).all()

@app.get("/students/{student_id}/subjects/{sub_id}/attendance-records", response_model=List[AttendanceRecordSchema])
def get_student_attendance_records(student_id: int, sub_id: int, db: Session = Depends(get_db)):
    return db.query(AttendanceRecordDB).filter(
        AttendanceRecordDB.studentId == student_id,
        AttendanceRecordDB.subjectId == sub_id
    ).order_by(AttendanceRecordDB.classDate.asc()).all()

@app.post("/attendance-records", response_model=AttendanceRecordSchema)
def upsert_attendance_record(rec: AttendanceRecordBase, db: Session = Depends(get_db)):
    existing = db.query(AttendanceRecordDB).filter(
        AttendanceRecordDB.studentId == rec.studentId,
        AttendanceRecordDB.subjectId == rec.subjectId,
        AttendanceRecordDB.classDate == rec.classDate
    ).first()

    data = to_dict(rec)
    if existing:
        for k, v in data.items():
            setattr(existing, k, v)
        db.commit()
        db.refresh(existing)
        return existing
    else:
        new_rec = AttendanceRecordDB(**data)
        db.add(new_rec)
        db.commit()
        db.refresh(new_rec)
        return new_rec

@app.get("/modification-logs/{entity_key:path}", response_model=ModificationLogSchema)
def get_modification_log(entity_key: str, db: Session = Depends(get_db)):
    log = db.query(ModificationLogDB).filter(ModificationLogDB.entity_key == entity_key).first()
    if not log:
        return ModificationLogSchema(
            entity_key=entity_key,
            log_text="Última modificación: Sin registros recientes",
            updated_at="",
            updated_by=""
        )
    return log

@app.post("/modification-logs", response_model=ModificationLogSchema)
def save_modification_log(log_in: ModificationLogBase, db: Session = Depends(get_db)):
    log = db.query(ModificationLogDB).filter(ModificationLogDB.entity_key == log_in.entity_key).first()
    if not log:
        log = ModificationLogDB(
            entity_key=log_in.entity_key,
            log_text=log_in.log_text,
            updated_at=log_in.updated_at,
            updated_by=log_in.updated_by
        )
        db.add(log)
    else:
        log.log_text = log_in.log_text
        log.updated_at = log_in.updated_at
        log.updated_by = log_in.updated_by
    db.commit()
    db.refresh(log)
    return log

def get_local_ip():
    try:
        import socket
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

if __name__ == "__main__":
    import uvicorn
    import copy
    from uvicorn.config import LOGGING_CONFIG

    def start_tunnel():
        import os
        import time
        time.sleep(2)
        tunnel_script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "run_tunnel_and_update_app.py")
        if os.path.exists(tunnel_script):
            try:
                import run_tunnel_and_update_app
                run_tunnel_and_update_app.main()
            except Exception as e:
                print(f"[!] Error al iniciar Cloudflare Tunnel: {e}")

    tunnel_thread = threading.Thread(target=start_tunnel, daemon=True)
    tunnel_thread.start()

    local_ip = get_local_ip()
    print("===================================================")
    print("    AULA VIRTUAL - SERVIDOR FASTAPI BACKEND")
    print("===================================================")
    print(f"Servidor FastAPI ejecutándose en el puerto 8000:")
    print(f"  • PC Localhost:            http://localhost:8000/")
    print(f"  • Emulador Android Studio: http://10.0.2.2:8000/")
    print(f"  • Red Local Wi-Fi:         http://{local_ip}:8000/")
    print("===================================================\n")

    # Configurar uvicorn para enviar logs informativos a sys.stdout (evita texto rojo en la consola del IDE)
    log_config = copy.deepcopy(LOGGING_CONFIG)
    if "handlers" in log_config:
        if "default" in log_config["handlers"]:
            log_config["handlers"]["default"]["stream"] = "ext://sys.stdout"
        if "access" in log_config["handlers"]:
            log_config["handlers"]["access"]["stream"] = "ext://sys.stdout"

    if "formatters" in log_config:
        if "default" in log_config["formatters"]:
            log_config["formatters"]["default"]["use_colors"] = True
        if "access" in log_config["formatters"]:
            log_config["formatters"]["access"]["use_colors"] = True

    uvicorn.run(app, host="0.0.0.0", port=8000, log_config=log_config)
