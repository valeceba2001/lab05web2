package teccr.justdoitcloud.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import teccr.justdoitcloud.data.Task;
import teccr.justdoitcloud.data.User;
import teccr.justdoitcloud.repository.TaskRepository;
import teccr.justdoitcloud.repository.UserRepository;
import teccr.justdoitcloud.service.external.taskgenerator.TaskGenerator;
import teccr.justdoitcloud.service.internal.taskarchiver.TaskArchiver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskGenerator taskGenerator;
    private final UserRepository userRepository;
    private final TaskArchiver taskArchiver;

    public TaskService(TaskRepository taskRepository,
                       TaskGenerator taskGenerator,
                       UserRepository userRepository,
                       TaskArchiver taskArchiver) {
        this.taskRepository = taskRepository;
        this.taskGenerator = taskGenerator;
        this.userRepository = userRepository;
        this.taskArchiver = taskArchiver;
    }

    public List<Task> getTasksForUser(User user) {
        return taskRepository.findByUserId(user.getId());
    }

    public Task addTaskToUser(User user, Task task) {
        task.setUserId(user.getId());
        task.setCreatedAt(LocalDateTime.now());
        Task taskCreated = taskRepository.save(task);

        Optional<User> maybeUser = userRepository.findById(user.getId());
        if (maybeUser.isPresent()) {
            taskArchiver.archiveTask("tasks-new", maybeUser.get(), taskCreated);
        }

        return taskCreated;
    }

    public Task autogenerateTaskForUser(User user) {
        // Pendiente: archivar la tarea en categoria "tasks-new" despues de creada

        Task task = taskGenerator.generateTask();
        if (task == null) {
            throw new RuntimeException("No se pudo generar la tarea automáticamente");
        }

        task.setUserId(user.getId());
        task.setCreatedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    public Optional<Task> getTaskById(Long id) {
        if (id == null || id < 0) {
            return Optional.empty();
        }
        return taskRepository.findById(id);
    }

    public Task updateTaskFields(Long id, Task updatedTask) {
        return taskRepository.findById(id)
                .map(existingTask -> {
                    if (updatedTask.getDescription() != null && !updatedTask.getDescription().trim().isEmpty()) {
                        existingTask.setDescription(updatedTask.getDescription().trim());
                    }
                    if (updatedTask.getStatus() != null) {
                        existingTask.setStatus(updatedTask.getStatus());
                    }
                    return taskRepository.save(existingTask);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    public void deleteTaskById(Long id) {
        Optional<Task> maybeTask = taskRepository.findById(id);
        if (maybeTask.isEmpty()) {
            throw new RuntimeException("Task not found with id: " + id);
        }

        Task task = maybeTask.get();

        Optional<User> maybeUser = userRepository.findById(task.getUserId());
        maybeUser.ifPresent(user -> {
            try {
                taskArchiver.archiveTask("tasks-deleted", user, task);
            } catch (Exception ignored) {
                log.error("Error archiving task with id {} for user id {}: {}", task.getId(), user.getId(), ignored.getMessage());
            }
        });

        taskRepository.deleteById(id);
    }

    // -----------------------------------------------------------------------
    // Métodos con validación de propiedad (Laboratorio 5)
    // -----------------------------------------------------------------------

    /**
     * Devuelve la tarea solo si pertenece al usuario indicado.
     * Si no existe o no pertenece al usuario → Optional vacío (→ 404 en el controlador).
     */
    public Optional<Task> getTaskByIdForUser(Long userId, Long taskId) {
        Optional<Task> taskOpt = getTaskById(taskId);
        if (taskOpt.isPresent() && taskOpt.get().getUserId().equals(userId)) {
            return taskOpt;
        }
        return Optional.empty();
    }

    /**
     * Actualiza la tarea solo si pertenece al usuario indicado.
     * Si no pertenece al usuario → Optional vacío (→ 403 en el controlador).
     */
    public Optional<Task> updateTaskFieldsForUser(Long userId, Long taskId, Task updatedTask) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty() || !taskOpt.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        Task existingTask = taskOpt.get();
        if (updatedTask.getDescription() != null && !updatedTask.getDescription().trim().isEmpty()) {
            existingTask.setDescription(updatedTask.getDescription().trim());
        }
        if (updatedTask.getStatus() != null) {
            existingTask.setStatus(updatedTask.getStatus());
        }
        return Optional.of(taskRepository.save(existingTask));
    }

    /**
     * Borra la tarea solo si pertenece al usuario indicado.
     * Devuelve true si se borró, false si no pertenece al usuario (→ 403 en el controlador).
     */
    public boolean deleteTaskByIdForUser(Long userId, Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty() || !taskOpt.get().getUserId().equals(userId)) {
            return false;
        }
        Task task = taskOpt.get();
        Optional<User> maybeUser = userRepository.findById(task.getUserId());
        maybeUser.ifPresent(user -> {
            try {
                taskArchiver.archiveTask("tasks-deleted", user, task);
            } catch (Exception ignored) {
                log.error("Error archiving task with id {} for user id {}: {}", task.getId(), user.getId(), ignored.getMessage());
            }
        });
        taskRepository.deleteById(taskId);
        return true;
    }

}
