from sqlalchemy import create_engine, text

def clean():
    engine = create_engine('mysql+mysqlconnector://root:@localhost/aula_virtual')
    with engine.connect() as conn:
        conn.execute(text("UPDATE users SET carnet = REPLACE(REPLACE(carnet, '\n', ''), '\r', '')"))
        conn.execute(text("UPDATE students SET carnet = REPLACE(REPLACE(carnet, '\n', ''), '\r', '')"))
        conn.commit()
        print("Carnets cleaned successfully.")

if __name__ == "__main__":
    clean()
