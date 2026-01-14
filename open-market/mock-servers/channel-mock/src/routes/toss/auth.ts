import { Router } from 'express';
import { authService } from '../../services/auth.service';

const router = Router();

router.post('/v1/auth/token', async (req, res) => {
  const { clientId, clientSecret } = req.body;

  if (!clientId || !clientSecret) {
    return res.status(400).json({ result: 'FAIL', message: '인증 정보가 필요합니다' });
  }

  const token = await authService.createToken('TOSS_STORE', 3600);

  return res.json({
    accessToken: token.token,
    expiresAt: new Date(Date.now() + token.expiresIn * 1000).toISOString(),
  });
});

export default router;
