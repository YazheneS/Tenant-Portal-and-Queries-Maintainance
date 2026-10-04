export default function StatusPill({ status }) {
  if (!status) return null;
  const key = status.toLowerCase();
  const label = status.charAt(0) + status.slice(1).toLowerCase().replace(/_/g, ' ');
  return <span className={`pill pill-${key}`}>{label}</span>;
}
