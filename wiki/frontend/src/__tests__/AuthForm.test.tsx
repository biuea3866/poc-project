import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthForm from '@/components/AuthForm';

// 라우터 모킹
jest.mock('next/navigation', () => ({
  useRouter: () => ({ replace: jest.fn() }),
}));

// auth 함수 모킹
const mockLogin = jest.fn();
const mockSignup = jest.fn();
jest.mock('@/lib/auth', () => ({
  login: (...args: unknown[]) => mockLogin(...args),
  signup: (...args: unknown[]) => mockSignup(...args),
}));

describe('AuthForm 컴포넌트', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('login 모드', () => {
    it('로그인 폼을 렌더링한다', () => {
      render(<AuthForm mode="login" />);

      expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument();
      expect(screen.queryByPlaceholderText('홍길동')).not.toBeInTheDocument();
    });

    it('이메일과 비밀번호를 입력할 수 있다', async () => {
      const user = userEvent.setup();
      render(<AuthForm mode="login" />);

      await user.type(screen.getByPlaceholderText('you@example.com'), 'test@example.com');
      await user.type(screen.getByPlaceholderText('••••••••'), 'password123');

      expect(screen.getByPlaceholderText('you@example.com')).toHaveValue('test@example.com');
      expect(screen.getByPlaceholderText('••••••••')).toHaveValue('password123');
    });

    it('폼 제출 시 login 함수를 호출한다', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValue(undefined);
      render(<AuthForm mode="login" />);

      await user.type(screen.getByPlaceholderText('you@example.com'), 'test@example.com');
      await user.type(screen.getByPlaceholderText('••••••••'), 'password123');
      await user.click(screen.getByRole('button', { name: '로그인' }));

      await waitFor(() => {
        expect(mockLogin).toHaveBeenCalledWith({
          email: 'test@example.com',
          password: 'password123',
        });
      });
    });

    it('로그인 실패 시 에러 메시지를 표시한다', async () => {
      const user = userEvent.setup();
      mockLogin.mockRejectedValue(new Error('이메일 또는 비밀번호가 올바르지 않습니다'));
      render(<AuthForm mode="login" />);

      await user.type(screen.getByPlaceholderText('you@example.com'), 'wrong@example.com');
      await user.type(screen.getByPlaceholderText('••••••••'), 'wrongpassword');
      await user.click(screen.getByRole('button', { name: '로그인' }));

      await waitFor(() => {
        expect(screen.getByText('이메일 또는 비밀번호가 올바르지 않습니다')).toBeInTheDocument();
      });
    });

    it('제출 중에는 버튼이 비활성화된다', async () => {
      mockLogin.mockImplementation(() => new Promise(() => {})); // 영원히 pending
      render(<AuthForm mode="login" />);

      fireEvent.change(screen.getByPlaceholderText('you@example.com'), {
        target: { value: 'test@example.com' },
      });
      fireEvent.change(screen.getByPlaceholderText('••••••••'), {
        target: { value: 'password123' },
      });
      fireEvent.submit(screen.getByRole('button', { name: '로그인' }).closest('form')!);

      await waitFor(() => {
        expect(screen.getByRole('button')).toBeDisabled();
        expect(screen.getByRole('button')).toHaveTextContent('처리 중...');
      });
    });
  });

  describe('signup 모드', () => {
    it('회원가입 폼을 렌더링한다', () => {
      render(<AuthForm mode="signup" />);

      expect(screen.getByPlaceholderText('홍길동')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '회원가입' })).toBeInTheDocument();
    });

    it('폼 제출 시 signup 함수를 호출한다', async () => {
      const user = userEvent.setup();
      mockSignup.mockResolvedValue(undefined);
      render(<AuthForm mode="signup" />);

      await user.type(screen.getByPlaceholderText('홍길동'), '홍길동');
      await user.type(screen.getByPlaceholderText('you@example.com'), 'new@example.com');
      await user.type(screen.getByPlaceholderText('••••••••'), 'newpassword123');
      await user.click(screen.getByRole('button', { name: '회원가입' }));

      await waitFor(() => {
        expect(mockSignup).toHaveBeenCalledWith({
          name: '홍길동',
          email: 'new@example.com',
          password: 'newpassword123',
        });
      });
    });
  });
});
