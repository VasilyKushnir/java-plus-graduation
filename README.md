# Comment API Documentation

## Overview

API комментариев позволяет пользователям добавлять, обновлять и удалять комментарии к опубликованным событиям. Комментарии проходят процедуру модерации, в ходе которой администраторы могут одобрять или отклонять их. Только одобренные комментарии видны в публичных эндпоинтах.
## Comment Statuses

Comments can have one of three statuses:

- **NEW** - Начальный статус при создании или обновлении комментария
- **APPROVED** - Комментарий был одобрен администратором
- **REJECTED** - Комментарий был отклонен администратором

## API Endpoints

### Public Endpoints

#### 1. Получение комментариев к мероприятию

Получение всех одобренных комментариев к конкретному событию с разбивкой по страницам.

**Endpoint:** `GET /events/{eventId}/comments`

**Path Parameters:**
- `eventId` (Long, required)

**Query Parameters:**
- `from` (int, default: 0)
- `size` (int, default: 10)

#### 2. Получение всех комментариев

Получение всех одобренных комментариев ко всем событиям с разбивкой по страницам.

**Endpoint:** `GET /comments`

**Query Parameters:**
- `from` (int, default: 0)
- `size` (int, default: 10)

### Private Endpoints (User)

#### 3. Добавление комментария


**Endpoint:** `POST /users/{userId}/events/{eventId}/comments`

**Path Parameters:**
- `userId` (Long, required)
- `eventId` (Long, required)

#### 4. Обновление комментария

Update an existing comment. Only the comment owner can update their comment. Updating a comment resets its status to `NEW` for moderation.

**Endpoint:** `PATCH /users/{userId}/comments/{commentId}`

**Path Parameters:**
- `userId` (Long, required)
- `commentId` (Long, required)

#### 5. Удаление комментария

**Endpoint:** `DELETE /users/{userId}/comments/{commentId}`

**Path Parameters:**
- `userId` (Long, required)
- `commentId` (Long, required)

### Admin Endpoints

#### 6. Одобрение комментария

Approve a comment, making it visible through public endpoints.

**Endpoint:** `PATCH /admin/comments/{commentId}/approve`

**Path Parameters:**
- `commentId` (Long, required)


#### 7. Отклонение комментария

Reject a comment, making it invisible through public endpoints.

**Endpoint:** `PATCH /admin/comments/{commentId}/reject`

**Path Parameters:**
- `commentId` (Long, required) - ID of the comment to reject

