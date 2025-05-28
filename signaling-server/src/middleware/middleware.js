const requestLogger = (req, res, next) => {
    console.log(`[${new Date(Date.now() + 7 * 60 * 60 * 1000).toISOString()}] ${req.method} ${req.url}`);
    next();
  };
  
const errorHandler = (err, req, res, next) => {
  console.error(`Error: ${err.message}`);
  res.status(500).json({ error: 'Internal server error' });
};

module.exports = { requestLogger, errorHandler };