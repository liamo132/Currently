export default function Dashboard() {
  const token = localStorage.getItem("token");
  return <div>Dashboard. Token present: {Boolean(token) ? "yes" : "no"}</div>;
}