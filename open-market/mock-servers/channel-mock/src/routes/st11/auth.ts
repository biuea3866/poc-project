import { Router } from 'express';
import { authService } from '../../services/auth.service';

const router = Router();

router.post('/auth/token', async (req, res) => {
  const { openapiKey, secretKey } = req.body;

  if (!openapiKey || !secretKey) {
    return res.status(400).json({
      code: '400',
      message: '인증 정보가 필요합니다',
    });
  }

  const token = await authService.createToken('ST11', 3600);

  return res.json({
    code: '200',
    message: '성공',
    data: {
      accessToken: token.token,
      expiresIn: token.expiresIn,
    },
  });
});

export default router;
