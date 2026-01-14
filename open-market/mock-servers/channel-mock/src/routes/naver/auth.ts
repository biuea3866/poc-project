import { Router } from 'express';
import { authService } from '../../services/auth.service';

const router = Router();

router.post('/oauth/token', async (req, res) => {
  const { client_id, client_secret } = req.body;

  if (!client_id || !client_secret) {
    return res.status(400).json({ error: 'invalid_client' });
  }

  const token = await authService.createToken('NAVER_STORE', 43200);

  return res.json({
    access_token: token.token,
    token_type: 'Bearer',
    expires_in: token.expiresIn,
  });
});

export default router;
