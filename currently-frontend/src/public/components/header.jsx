import React from 'react';
import { Link } from 'react-router-dom';
import "../../styles/global.css";

const Header = () => {
  return (
    <header>
      <nav>
        <ul>
          <li><a href="#">More Information</a></li>
          <li><Link to="/login">Login/Signup</Link></li>
        </ul>
        <Link to="/" className="logo">Currently.</Link>
      </nav>
    </header>
  );
};

export default Header;