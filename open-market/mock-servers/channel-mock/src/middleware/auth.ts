import { NextFunction, Request, Response } from 'express';
import { Channel } from '../types';
import { authService } from '../services/auth.service';

function parseAuthToken(headerValue?: string): string | null {
  if (!headerValue) {
    return null;
  }

  if (headerValue.startsWith('Bearer ')) {
    return headerValue.replace('Bearer ', '').trim();
  }

  return headerValue.trim();
}

export function requireAuth(channel: Channel, options: { allowMissing?: boolean } = {}) {
  return async (req: Request, res: Response, next: NextFunction) => {
    const scenario = req.headers['x-mock-scenario'];
    if (scenario === 'error-auth') {
      return res.status(401).json({ code: '401', message: '인증 실패' });
    }

    const token = parseAuthToken(req.headers.authorization as string | undefined);

    if (!token) {
      if (options.allowMissing) {
        return next();
      }
      return res.status(401).json({ code: '401', message: '인증 토큰이 필요합니다' });
    }

    const isValid = await authService.validateToken(channel, token);
    if (!isValid && !options.allowMissing) {
      return res.status(401).json({ code: '401', message: '유효하지 않은 토큰입니다' });
    }

    return next();
  };
}
