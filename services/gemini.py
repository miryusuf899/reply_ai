import time
import requests
from django.conf import settings


class GeminiService:
    """
    AI сервис через Groq API.
    Llama 3.3 70B — быстро и бесплатно.
    """
    MODEL_NAME = 'llama-3.3-70b-versatile'
    API_URL = 'https://api.groq.com/openai/v1/chat/completions'

    TONE_INSTRUCTIONS = {
        'formal': 'Отвечай строго формально, профессионально, без сокращений.',
        'friendly': 'Отвечай дружески, тепло, как близкий друг.',
        'neutral': 'Отвечай нейтрально, без лишних эмоций.',
        'assertive': 'Отвечай уверенно, чётко, без лишних слов.',
        'empathetic': 'Отвечай с пониманием и сочувствием, мягко.',
    }

    REQUEST_TYPE_INSTRUCTIONS = {
        'reply_help': (
            'Ты помогаешь написать ответ в мессенджере. '
            'Проанализируй переписку и напиши готовый ответ. '
            'Верни ТОЛЬКО текст ответа, без пояснений.'
        ),
        'translation': (
            'Ты переводчик. Переведи текст пользователя на указанный язык. '
            'Верни ТОЛЬКО перевод, без пояснений.'
        ),
        'rewrite': (
            'Перепиши текст пользователя, сохранив смысл но улучшив стиль. '
            'Верни ТОЛЬКО переписанный текст.'
        ),
        'tone_change': (
            'Измени тон текста пользователя на указанный. '
            'Верни ТОЛЬКО изменённый текст.'
        ),
        'analyze': (
            'Проанализируй переписку и кратко опиши суть разговора, '
            'настроение собеседника и ключевые темы. '
            'Ответ на языке пользователя.'
        ),
    }

    def __init__(self):
        self.api_key = settings.GEMINI_API_KEY
        self.headers = {
            'Authorization': f'Bearer {self.api_key}',
            'Content-Type': 'application/json',
        }

    def _build_system_prompt(self, request_type, tone, target_language):
        base = self.REQUEST_TYPE_INSTRUCTIONS.get(request_type, '')
        tone_instruction = self.TONE_INSTRUCTIONS.get(tone, '')
        language_instruction = (
            f'Отвечай на языке: {target_language}.'
            if target_language and target_language != 'auto'
            else ''
        )
        parts = [p for p in [base, tone_instruction, language_instruction] if p]
        return ' '.join(parts)

    def _format_chat_context(self, messages):
        if not messages:
            return ''
        lines = []
        for msg in messages:
            sender = 'Я' if msg['sender'] == 'me' else 'Собеседник'
            lines.append(f'{sender}: {msg["content"]}')
        return '\n'.join(lines)

    def _call_api(self, system_prompt, user_content):
        response = requests.post(
            self.API_URL,
            headers=self.headers,
            json={
                'model': self.MODEL_NAME,
                'messages': [
                    {'role': 'system', 'content': system_prompt},
                    {'role': 'user', 'content': user_content},
                ],
                'max_tokens': 1000,
                'temperature': 0.7,
            },
            timeout=30,
        )
        response.raise_for_status()
        data = response.json()
        if 'error' in data:
            raise Exception(data['error'].get('message', 'API error'))
        return data

    def generate_response(
        self,
        messages,
        user_prompt,
        request_type,
        tone='neutral',
        target_language='ru',
    ):
        start_time = time.time()

        system_prompt = self._build_system_prompt(request_type, tone, target_language)
        chat_context = self._format_chat_context(messages)

        parts = []
        if chat_context:
            parts.append(f'[Переписка]:\n{chat_context}')
        if user_prompt:
            parts.append(f'[Задача]: {user_prompt}')

        full_prompt = '\n\n'.join(parts)

        try:
            data = self._call_api(system_prompt, full_prompt)
            generated_text = data['choices'][0]['message']['content']
            tokens_used = data.get('usage', {}).get('total_tokens', 0)
        except Exception as e:
            generated_text = f'Ошибка генерации: {str(e)}'
            tokens_used = 0

        time_ms = int((time.time() - start_time) * 1000)

        return {
            'text': generated_text,
            'model': self.MODEL_NAME,
            'tokens': tokens_used,
            'time_ms': time_ms,
        }

    def analyze_chat_context(self, messages):
        if not messages:
            return ''
        chat_context = self._format_chat_context(messages)
        prompt = (
            'Кратко (2-3 предложения) опиши суть этой переписки '
            f'и настроение собеседников:\n\n{chat_context}'
        )
        try:
            data = self._call_api(
                'Ты аналитик переписок. Отвечай кратко на русском.',
                prompt
            )
            return data['choices'][0]['message']['content']
        except Exception:
            return ''