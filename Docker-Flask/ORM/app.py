from flask import Flask, jsonify, request
from flask_sqlalchemy import SQLAlchemy
from flask_bcrypt import Bcrypt
from functools import wraps
from datetime import datetime, timedelta, timezone
import os
import jwt
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)

# 1. Configuración de la Base de Datos (SQLite)
# El archivo se guardará en la carpeta del contenedor como 'site.db'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///site.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

# La llave con la que se firman los tokens JWT. Debe venir de una variable de
# entorno (ver .env.example); NUNCA debe quedar escrita en el código en un
# despliegue real. Si no existe, se usa un valor de desarrollo.
SECRET_KEY = os.environ.get('SECRET_KEY', 'dev-secret-CHANGE-ME')
app.config['SECRET_KEY'] = SECRET_KEY

# Tiempo de vida del token de sesión
TOKEN_EXPIRATION_HOURS = 2

db = SQLAlchemy(app)
bcrypt = Bcrypt(app)

# 2. Modelos (las tablas en la BD)


class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(20), unique=True, nullable=False)
    password = db.Column(db.String(60), nullable=False)  # Aquí guardaremos el hash

    def __repr__(self):
        return f"User('{self.username}')"


class Task(db.Model):
    """Recurso sobre el que se realizan las operaciones CRUD de la práctica."""
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(100), nullable=False)
    description = db.Column(db.String(500), nullable=True, default="")
    completed = db.Column(db.Boolean, nullable=False, default=False)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    created_at = db.Column(db.DateTime, default=lambda: datetime.now(timezone.utc))

    def to_dict(self):
        return {
            "id": self.id,
            "title": self.title,
            "description": self.description,
            "completed": self.completed,
            "user_id": self.user_id,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }


# 3. Utilidades de autenticación (sesiones seguras vía JWT)


def generate_token(user_id: int) -> str:
    payload = {
        "user_id": user_id,
        "exp": datetime.now(timezone.utc) + timedelta(hours=TOKEN_EXPIRATION_HOURS),
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, SECRET_KEY, algorithm="HS256")


def token_required(f):
    """Protege un endpoint: exige un header 'Authorization: Bearer <token>'
    válido y no expirado. Si todo es correcto, inyecta el usuario actual
    como primer argumento de la vista."""

    @wraps(f)
    def decorated(*args, **kwargs):
        token = None
        auth_header = request.headers.get('Authorization', '')
        if auth_header.startswith('Bearer '):
            token = auth_header.split(' ', 1)[1].strip()

        if not token:
            return jsonify({"message": "Falta el token de autenticación"}), 401

        try:
            data = jwt.decode(token, SECRET_KEY, algorithms=["HS256"])
        except jwt.ExpiredSignatureError:
            return jsonify({"message": "El token ha expirado, inicia sesión de nuevo"}), 401
        except jwt.InvalidTokenError:
            return jsonify({"message": "Token inválido"}), 401

        current_user = db.session.get(User, data.get('user_id'))
        if not current_user:
            return jsonify({"message": "Usuario no válido"}), 401

        return f(current_user, *args, **kwargs)

    return decorated


# 4. Rutas generales / autenticación

@app.route('/')
def hello():
    return jsonify({"message": "API Funcionando"})


@app.route('/register', methods=['POST'])
def register():
    data = request.get_json(silent=True) or {}
    username = data.get('username')
    password = data.get('password')

    if not username or not password:
        return jsonify({"message": "username y password son obligatorios"}), 400

    if User.query.filter_by(username=username).first():
        return jsonify({"message": "El usuario ya existe"}), 400

    hashed_password = bcrypt.generate_password_hash(password).decode('utf-8')

    new_user = User(username=username, password=hashed_password)
    db.session.add(new_user)
    db.session.commit()

    return jsonify({"message": "Usuario creado exitosamente"}), 201


@app.route('/login', methods=['POST'])
def login():
    data = request.get_json(silent=True) or {}
    username = data.get('username')
    password = data.get('password')

    if not username or not password:
        return jsonify({"status": "error", "message": "username y password son obligatorios"}), 400

    user = User.query.filter_by(username=username).first()

    if user and bcrypt.check_password_hash(user.password, password):
        token = generate_token(user.id)
        return jsonify({
            "status": "success",
            "message": "Login exitoso",
            "user_id": user.id,
            "username": user.username,
            "token": token,
        }), 200
    else:
        return jsonify({"status": "error", "message": "Credenciales inválidas"}), 401


# 5. Operaciones CRUD sobre el recurso "Task" (protegidas con token)

@app.route('/tasks', methods=['GET'])
@token_required
def get_tasks(current_user):
    tasks = (
        Task.query.filter_by(user_id=current_user.id)
        .order_by(Task.created_at.desc())
        .all()
    )
    return jsonify([t.to_dict() for t in tasks]), 200


@app.route('/tasks/<int:task_id>', methods=['GET'])
@token_required
def get_task(current_user, task_id):
    task = Task.query.filter_by(id=task_id, user_id=current_user.id).first()
    if not task:
        return jsonify({"message": "Tarea no encontrada"}), 404
    return jsonify(task.to_dict()), 200


@app.route('/tasks', methods=['POST'])
@token_required
def create_task(current_user):
    data = request.get_json(silent=True) or {}
    title = data.get('title')
    if not title:
        return jsonify({"message": "El campo 'title' es obligatorio"}), 400

    task = Task(
        title=title,
        description=data.get('description', '') or '',
        completed=bool(data.get('completed', False)),
        user_id=current_user.id,
    )
    db.session.add(task)
    db.session.commit()
    return jsonify(task.to_dict()), 201


@app.route('/tasks/<int:task_id>', methods=['PUT'])
@token_required
def update_task(current_user, task_id):
    task = Task.query.filter_by(id=task_id, user_id=current_user.id).first()
    if not task:
        return jsonify({"message": "Tarea no encontrada"}), 404

    data = request.get_json(silent=True) or {}
    if 'title' in data:
        if not data['title']:
            return jsonify({"message": "El campo 'title' no puede estar vacío"}), 400
        task.title = data['title']
    if 'description' in data:
        task.description = data['description'] or ''
    if 'completed' in data:
        task.completed = bool(data['completed'])

    db.session.commit()
    return jsonify(task.to_dict()), 200


@app.route('/tasks/<int:task_id>', methods=['DELETE'])
@token_required
def delete_task(current_user, task_id):
    task = Task.query.filter_by(id=task_id, user_id=current_user.id).first()
    if not task:
        return jsonify({"message": "Tarea no encontrada"}), 404

    db.session.delete(task)
    db.session.commit()
    return jsonify({"message": "Tarea eliminada"}), 200


if __name__ == '__main__':
    # Esto crea las tablas automáticamente si no existen al iniciar
    with app.app_context():
        db.create_all()

    app.run(host='0.0.0.0', port=5000, debug=True)
