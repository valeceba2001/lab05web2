-- Usuario 1: john_doe
INSERT INTO users (user_name, name, email, type)
VALUES ('john_doe', 'John Doe', 'john@gmail.com', 'REGULAR');

-- Usuario 2: jane_smith
INSERT INTO users (user_name, name, email, type)
VALUES ('jane_smith', 'Jane Smith', 'jane@gmail.com', 'REGULAR');

-- Tareas del usuario 1 (john_doe) → id=1 y id=2
INSERT INTO tasks (user_id, description, status) VALUES (1, 'Comprar leche', 'PENDING');
INSERT INTO tasks (user_id, description, status) VALUES (1, 'Reparar llantas del carro', 'INPROGRESS');

-- Tareas del usuario 2 (jane_smith) → id=3 y id=4
INSERT INTO tasks (user_id, description, status) VALUES (2, 'Estudiar para el examen', 'PENDING');
INSERT INTO tasks (user_id, description, status) VALUES (2, 'Entregar el laboratorio', 'INPROGRESS');
